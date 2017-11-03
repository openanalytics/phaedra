package eu.openanalytics.phaedra.base.fs.smb;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import eu.openanalytics.phaedra.base.fs.Activator;
import eu.openanalytics.phaedra.base.fs.BaseFileServer;
import eu.openanalytics.phaedra.base.fs.SMBHelper;
import eu.openanalytics.phaedra.base.fs.preferences.Prefs;
import eu.openanalytics.phaedra.base.util.io.FileUtils;
import eu.openanalytics.phaedra.base.util.io.StreamUtils;
import eu.openanalytics.phaedra.base.util.process.ProcessUtils;
import jcifs.UniAddress;
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbSession;

public class SMBInterface extends BaseFileServer {

	private UniAddress domain;
	private NtlmPasswordAuthentication auth;
	
	private String basePath;
	private String privateMount;
	
	@Override
	public boolean isCompatible(String fsPath, String userName) {
		return SMBHelper.isSMBPath(fsPath);
	}
	
	@Override
	public void initialize(String fsPath, String userName, String pw) throws IOException {
		basePath = SMBHelper.toSMBNotation(fsPath);
		String serverName = basePath.substring(SMBHelper.SMB_PROTOCOL_PREFIX.length()).split("/")[0];
		
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
//		if (wins != null && !wins.isEmpty()) jcifs.Config.setProperty("jcifs.netbios.wins", wins);
		
		login(serverName, userName, pw);
	}
	
	@Override
	public void close() throws IOException {
		if (privateMount != null) unmount();
	}

	@Override
	public long getFreeSpace() throws IOException {
		SmbFile sFile = getFile("/", false);
		return sFile.getDiskFreeSpace();
	}
	
	@Override
	public long getTotalSpace() throws IOException {
		SmbFile sFile = getFile("/", false);
		return sFile.length();
	}
	
	@Override
	public boolean exists(String path) throws IOException {
		SmbFile sFile = getFile(path, false);
		return sFile.exists();
	}
	
	@Override
	public boolean isDirectory(String path) throws IOException {
		if (!path.endsWith("/")) path += "/";
		SmbFile sFile = getFile(path, false);
		return sFile.isDirectory();
	}
	
	@Override
	public long getCreateTime(String path) throws IOException {
		SmbFile sFile = getFile(path, false);
		return sFile.createTime();
	}
	
	@Override
	public long getLastModified(String path) throws IOException {
		SmbFile sFile = getFile(path, false);
		return sFile.lastModified();
	}
	
	@Override
	public List<String> dir(String path) throws IOException {
		if (!path.endsWith("/")) path += "/";
		SmbFile sFile = getFile(path, false);
		String[] children = sFile.list();
		return Arrays.asList(children);
	}
	
	@Override
	public void mkDir(String path) throws IOException {
		if (!path.endsWith("/")) path += "/";
		SmbFile sFile = getFile(path, false);
		sFile.mkdir();
	}
	
	@Override
	public void mkDirs(String path) throws IOException {
		if (!path.endsWith("/")) path += "/";
		SmbFile sFile = getFile(path, false);
		sFile.mkdirs();
	}

	@Override
	public void delete(String path) throws IOException {
		SmbFile sFile = getFile(path, false);
		sFile.delete();
	}
	
	@Override
	public long getLength(String path) throws IOException {
		SmbFile sFile = getFile(path, false);
		return sFile.length();
	}
	
	@Override
	public InputStream getInputStream(String path) throws IOException {
		SmbFile sFile = getFile(path, false);
		return sFile.getInputStream();
	}

	@Override
	public SeekableByteChannel getChannel(String path, String mode) throws IOException {
		//TODO SeekableSMBFile seems to have performance issues. Prefer UNC access on Windows.
		if (ProcessUtils.isWindows() && !mode.toLowerCase().contains("w")) {
			Set<OpenOption> opts = new HashSet<>();
			opts.add(StandardOpenOption.READ);
			return Files.newByteChannel(Paths.get(SMBHelper.toUNCNotation(getFullPath(path))), opts);
		} else {
			return new SeekableSMBFile(getFile(path, mode.toLowerCase().contains("w")), mode);
		}
	}
	
	@Override
	public File getAsFile(String path) {
		if (ProcessUtils.isWindows()) {
			return new File(SMBHelper.toUNCNotation(getFullPath(path)));
		} else {
			if (privateMount == null) mount();
			return new File(privateMount + "/" + path);
		}
	}
	
	@Override
	protected void doRenameTo(String from, String to) throws IOException {
		SmbFile oldSmb = getFile(from, false);
		SmbFile newSmb = getFile(to, false);
		oldSmb.renameTo(newSmb);
	}
	
	@Override
	protected void doUpload(String path, InputStream input) throws IOException {
		SmbFile sFile = getFile(path, true);
		StreamUtils.copyAndClose(input, sFile.getOutputStream());
	}
	
	/*
	 * Non-public
	 * **********
	 */
	
	private void login(String address, String userName, String pw) throws IOException {
		domain = UniAddress.getByName(address);
		auth = new NtlmPasswordAuthentication(address, userName, pw);
		SmbSession.logon(domain, auth);
	}
	
	private String getFullPath(String path) {
		if (!path.startsWith("/") && !path.startsWith("\\")) path = "/" + path;
		if (basePath.endsWith("/") || basePath.endsWith("\\")) return basePath + path;
		else return basePath + "/" + path;
	}
	
	private SmbFile getFile(String path, boolean lock) throws MalformedURLException {
		return SMBHelper.getFile(getFullPath(path), auth, lock);
	}
	
	private void mount() {
		if (ProcessUtils.isMac()) {
			// On Mac, make a private non-sudo mount
			privateMount = System.getProperty("java.io.tmpdir") + "/mnt_" + UUID.randomUUID().toString();
			new File(privateMount).mkdirs();
			
			String smbPath = String.format("//%s;%s:%s@%s", auth.getDomain(), auth.getUsername(), auth.getPassword(), basePath.substring(SMBHelper.SMB_PROTOCOL_PREFIX.length()));
			String[] cmd = { "mount", "-t", "smbfs", smbPath, privateMount };
			try {
				ProcessUtils.execute(cmd, null, null, true, true);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		} else {
			// On Linux, depend on the system property pointing to a local path or dedicated sudo mount
			privateMount = System.getProperty("phaedra.fs.path");
		}
	}
	
	private void unmount() {
		if (ProcessUtils.isMac()) {
			try {
				int retCode = ProcessUtils.execute(new String[] { "umount", privateMount }, null, null, true, true);
				if (retCode == 0) FileUtils.deleteRecursive(privateMount);
			} catch (InterruptedException e) {}
		}
	}
}
