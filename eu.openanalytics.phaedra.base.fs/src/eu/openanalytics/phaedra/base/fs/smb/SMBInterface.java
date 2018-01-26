package eu.openanalytics.phaedra.base.fs.smb;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
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
		
		login(getHostname(), userName, pw);
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
		//TODO SeekableSMBFile is very slow. Avoid using it if possible.
		if (mode.toLowerCase().contains("w")) {
			return new SeekableSMBFile(getFile(path, true), mode);
		} else {
			Set<OpenOption> opts = new HashSet<>();
			opts.add(StandardOpenOption.READ);
			String filePath = getAsFile(path).getAbsolutePath();
			return Files.newByteChannel(Paths.get(filePath), opts);
		}
	}
	
	@Override
	public File getAsFile(String path) {
		return new File(getMount() + "/" + path);
	}
	
	@Override
	protected void doRenameTo(String from, String to) throws IOException {
		SmbFile oldSmb = getFile(from, false);
		SmbFile newSmb = getFile(to, false);
		oldSmb.renameTo(newSmb);
	}
	
	@Override
	protected void doUpload(String path, InputStream input, long length) throws IOException {
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
	
	private String getHostname() {
		return basePath.substring(SMBHelper.SMB_PROTOCOL_PREFIX.length()).split("/")[0];
	}
	
	private synchronized String getMount() {
		if (privateMount == null) mount();
		return privateMount;
	}
	
	private synchronized void mount() {
		if (privateMount != null) return;
		
		if (ProcessUtils.isWindows()) {
			// Mapping won't work if another mapping exists to the same server!
			// See https://support.microsoft.com/en-us/help/938120/error-message-when-you-use-user-credentials-to-connect-to-a-network-sh
			// Workaround: map with ip address instead of hostname
			try {
				InetAddress ip = InetAddress.getByName(getHostname());
				privateMount = SMBHelper.toUNCNotation(basePath).replace(getHostname(), ip.getHostAddress()).replace('/', '\\');
			} catch (UnknownHostException e) {
				throw new RuntimeException(e);
			}
			String[] cmd = { "net", "use", privateMount, auth.getPassword(), "/user:" + auth.getUsername()};
			try {
				ProcessUtils.execute(cmd, null, null, true, true);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		} else if (ProcessUtils.isMac()) {
			// On Mac, make a private non-sudo mount
			privateMount = System.getProperty("java.io.tmpdir") + "/mnt_" + UUID.randomUUID().toString();
			new File(privateMount).mkdirs();
			
			//TODO Mount fails if password contains special characters
			String smbPath = String.format("//%s;%s:%s@%s", auth.getDomain(), auth.getUsername(), auth.getPassword(), basePath.substring(SMBHelper.SMB_PROTOCOL_PREFIX.length()));
			String[] cmd = { "mount", "-t", "smbfs", smbPath, privateMount };
			try {
				ProcessUtils.execute(cmd, null, null, true, true);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		} else {
			// On Linux, depend on the system property pointing to a local path or dedicated sudo mount
			String sysProp = "phaedra.fs.path";
			privateMount = System.getProperty(sysProp);
			if (privateMount == null || privateMount.isEmpty()) throw new RuntimeException("Cannot access SMB file: please set the " + sysProp + "system property.");
		}
	}
	
	private synchronized void unmount() {
		if (privateMount == null) return;
		
		if (ProcessUtils.isWindows()) {
			try {
				ProcessUtils.execute(new String[] { "net", "use", privateMount, "/delete", "/yes" }, null, null, true, true);
			} catch (Exception e) {}
		} else if (ProcessUtils.isMac()) {
			try {
				int retCode = ProcessUtils.execute(new String[] { "umount", privateMount }, null, null, true, true);
				if (retCode == 0) FileUtils.deleteRecursive(privateMount);
			} catch (Exception e) {}
		}
		
		privateMount = null;
	}
}
