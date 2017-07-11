package eu.openanalytics.phaedra.base.fs.smb;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.nio.channels.SeekableByteChannel;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import eu.openanalytics.phaedra.base.fs.Activator;
import eu.openanalytics.phaedra.base.fs.FSInterface;
import eu.openanalytics.phaedra.base.fs.SecureFileServer;
import eu.openanalytics.phaedra.base.fs.preferences.Prefs;
import eu.openanalytics.phaedra.base.util.io.FileUtils;
import eu.openanalytics.phaedra.base.util.process.ProcessUtils;
import jcifs.UniAddress;
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbSession;

public class SMBInterface implements FSInterface {

	private static final String SMB_PROTOCOL_PREFIX = "smb://";
	
	private UniAddress domain;
	private NtlmPasswordAuthentication auth;
	
	private String basePath;
	private String privateMount;
	
	@Override
	public boolean isCompatible(String fsPath, String userName) {
		// Only supports UNC locations.
		return (fsPath.startsWith(SecureFileServer.UNC_PREFIX));
	}
	
	@Override
	public void initialize(String fsPath, String userName, String pw, String wins) throws IOException {
		// Set timeouts based on user preferences.
		int smbSocketTimeout = Activator.getDefault().getPreferenceStore().getInt(Prefs.SMB_SOCKET_TIMEOUT);
		int smbResponseTimeout = Activator.getDefault().getPreferenceStore().getInt(Prefs.SMB_RESPONSE_TIMEOUT);
		jcifs.Config.setProperty("jcifs.smb.client.soTimeout", "" + smbSocketTimeout); // Default: 35000
		jcifs.Config.setProperty("jcifs.smb.client.responseTimeout", "" + smbResponseTimeout); // Default: 30000
		
		// Increase buffer sizes from default 65kb to 1mb.
		jcifs.Config.setProperty("jcifs.smb.client.rcv_buf_size", "1048576");
		jcifs.Config.setProperty("jcifs.smb.client.snd_buf_size", "1048576");
		jcifs.Config.setProperty("jcifs.smb.client.transaction_buf_size", "1048576");
		
		// Use WINS servers if the file server is in a different subnet.
		if (wins != null && !wins.isEmpty()) jcifs.Config.setProperty("jcifs.netbios.wins", wins);
		
		fsPath = fsPath.substring(SecureFileServer.UNC_PREFIX.length()).replace('\\', '/');
		String serverName = fsPath.split("/")[0];
		
		basePath = SecureFileServer.UNC_PREFIX + fsPath;
		
		login(serverName, userName, pw);
	}
	
	@Override
	public void close() throws IOException {
		if (privateMount != null) unmount();
	}
	
	public void login(String address, String userName, String pw) throws IOException {
		domain = UniAddress.getByName(address);
		auth = new NtlmPasswordAuthentication(address, userName, pw);
		SmbSession.logon(domain, auth);
	}

	public boolean exists(String path) throws IOException {
		SmbFile sFile = getFile(path, false);
		return sFile.exists();
	}
	
	public boolean isDirectory(String path) throws IOException {
		if (!path.endsWith("/")) path += "/";
		SmbFile sFile = getFile(path, false);
		return sFile.isDirectory();
	}
	
	public long getCreateTime(String path) throws IOException {
		SmbFile sFile = getFile(path, false);
		return sFile.createTime();
	}
	
	public long getLastModified(String path) throws IOException {
		SmbFile sFile = getFile(path, false);
		return sFile.lastModified();
	}
	
	public List<String> dir(String path) throws IOException {
		if (!path.endsWith("/")) path += "/";
		SmbFile sFile = getFile(path, false);
		String[] children = sFile.list();
		return Arrays.asList(children);
	}
	
	public void mkDir(String path) throws IOException {
		if (!path.endsWith("/")) path += "/";
		SmbFile sFile = getFile(path, false);
		sFile.mkdir();
	}
	
	public void mkDirs(String path) throws IOException {
		if (!path.endsWith("/")) path += "/";
		SmbFile sFile = getFile(path, false);
		sFile.mkdirs();
	}

	public void delete(String path) throws IOException {
		SmbFile sFile = getFile(path, false);
		sFile.delete();
	}
	
	public void renameTo(String oldPath, String newPath) throws IOException {
		SmbFile oldSmb = getFile(oldPath, false);
		SmbFile newSmb = getFile(newPath, false);
		oldSmb.renameTo(newSmb);
	}
	
	public long getLength(String path) throws IOException {
		SmbFile sFile = getFile(path, false);
		return sFile.length();
	}
	
	public InputStream getInputStream(String path) throws IOException {
		SmbFile sFile = getFile(path, false);
		return sFile.getInputStream();
	}

	public OutputStream getOutputStream(String path) throws IOException {
		SmbFile sFile = getFile(path, true);
		return sFile.getOutputStream();
	}
	
	@Override
	public SeekableByteChannel getChannel(String path, String mode) throws IOException {
		return new SeekableSMBFile(getFile(path, mode.toLowerCase().contains("w")), mode);
	}
	
	@Override
	public File getAsFile(String path) {
		if (path.startsWith(SecureFileServer.UNC_PREFIX) && !ProcessUtils.isWindows()) {
			if (privateMount == null) mount();
			return new File(privateMount + path.substring(basePath.length()));
		} else {
			return new File(path);
		}
	}
	
	/*
	 * Non-public
	 * **********
	 */
	
	private SmbFile getFile(String path, boolean lock) throws MalformedURLException {
		int sharing = SmbFile.FILE_SHARE_READ;
		if (!lock) sharing = sharing | SmbFile.FILE_SHARE_WRITE | SmbFile.FILE_SHARE_DELETE;
		
		if (path.startsWith(SecureFileServer.UNC_PREFIX)) {
			path = path.substring(SecureFileServer.UNC_PREFIX.length());
		}
		path = path.replace('\\', '/');
		String smbPath = SMB_PROTOCOL_PREFIX + path;
		
		SmbFile sFile = new SmbFile(smbPath, auth, sharing);
		return sFile;
	}
	
	private void mount() {
		privateMount = System.getProperty("java.io.tmpdir") + "/mnt_" + UUID.randomUUID().toString();
		new File(privateMount).mkdirs();
		
		String smbPath = "//"+ auth.getDomain() + ";"+ auth.getUsername() + ":"+ auth.getPassword() + "@"+ basePath.substring(2);
		
		String[] cmd = { "mount", "-t", "smbfs", smbPath, privateMount };
		try {
			ProcessUtils.execute(cmd, null, null, true, true);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}
	
	private void unmount() {
		try {
			int retCode = ProcessUtils.execute(new String[] { "umount", privateMount }, null, null, true, true);
			if (retCode == 0) FileUtils.deleteRecursive(privateMount);
		} catch (InterruptedException e) {}
	}
}
