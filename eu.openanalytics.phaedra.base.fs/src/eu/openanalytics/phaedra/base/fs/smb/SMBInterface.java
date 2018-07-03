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
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.eclipse.jface.preference.IPreferenceStore;

import eu.openanalytics.phaedra.base.fs.Activator;
import eu.openanalytics.phaedra.base.fs.BaseFileServer;
import eu.openanalytics.phaedra.base.fs.FileServerConfig;
import eu.openanalytics.phaedra.base.fs.SMBHelper;
import eu.openanalytics.phaedra.base.fs.preferences.Prefs;
import eu.openanalytics.phaedra.base.util.io.FileUtils;
import eu.openanalytics.phaedra.base.util.io.StreamUtils;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.base.util.process.ProcessUtils;
import jcifs.CIFSContext;
import jcifs.config.PropertyConfiguration;
import jcifs.context.BaseContext;
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbFile;

public class SMBInterface extends BaseFileServer {

	private String basePath;
	private String privateMount;
	
	private CIFSContext context;
	
	@Override
	public boolean isCompatible(FileServerConfig cfg) {
		String fsPath = cfg.get(FileServerConfig.PATH);
		return SMBHelper.isSMBPath(fsPath);
	}
	
	@Override
	public void initialize(FileServerConfig cfg) throws IOException {
		String fsPath = cfg.get(FileServerConfig.PATH);
		String userName = cfg.get(FileServerConfig.USERNAME);
		String pw = cfg.getEncrypted(FileServerConfig.PASSWORD);
		
		basePath = SMBHelper.toSMBNotation(fsPath);
		
		Properties p = new Properties();
		if (Boolean.valueOf(cfg.get("enable.smb2"))) p.setProperty("jcifs.smb.client.enableSMB2", "true");
		
		// Authentication information
		p.setProperty("jcifs.smb.client.username", userName.contains("\\") ? userName.substring(userName.indexOf('\\') + 1) : userName);
		p.setProperty("jcifs.smb.client.password", pw);
		p.setProperty("jcifs.smb.client.domain", userName.contains("\\") ? userName.substring(0, userName.indexOf('\\')) : "");
		
		// Increase buffer sizes from default 65kb to 1mb.
		p.setProperty("jcifs.smb.client.rcv_buf_size", "1048576");
		p.setProperty("jcifs.smb.client.snd_buf_size", "1048576");
		p.setProperty("jcifs.smb.client.transaction_buf_size", "1048576");

		// Set timeouts as given in the user's preferences.
		IPreferenceStore prefs = Activator.getDefault().getPreferenceStore();
		p.setProperty("jcifs.smb.client.soTimeout", String.valueOf(prefs.getInt(Prefs.SMB_SOCKET_TIMEOUT))); // Default: 35000
		p.setProperty("jcifs.smb.client.responseTimeout", String.valueOf(prefs.getInt(Prefs.SMB_RESPONSE_TIMEOUT))); // Default: 30000
		
		context = new BaseContext(new PropertyConfiguration(p));
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
		return Arrays.asList(children).stream().map(c -> c.endsWith("/") ? c.substring(0, c.length()-1) : c).collect(Collectors.toList());
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
	
	private String getFullPath(String path) {
		if (!path.startsWith("/") && !path.startsWith("\\")) path = "/" + path;
		if (basePath.endsWith("/") || basePath.endsWith("\\")) return basePath + path;
		else return basePath + "/" + path;
	}
	
	private SmbFile getFile(String path, boolean lock) throws MalformedURLException {
		return new SmbFile(getFullPath(path), context);
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
		NtlmPasswordAuthentication auth = (NtlmPasswordAuthentication) context.getCredentials();
		
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
			String smbPath = String.format("//%s;%s:%s@%s", auth.getUserDomain(), auth.getUsername(), auth.getPassword(), basePath.substring(SMBHelper.SMB_PROTOCOL_PREFIX.length()));
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
		
		try {
			if (ProcessUtils.isWindows()) {
				ProcessUtils.execute(new String[] { "net", "use", privateMount, "/delete", "/yes" }, null, null, true, true);
			} else if (ProcessUtils.isMac()) {
				int retCode = ProcessUtils.execute(new String[] { "umount", privateMount }, null, null, true, true);
				if (retCode == 0) FileUtils.deleteRecursive(privateMount);
			}
		} catch (Throwable e) {
			EclipseLog.warn("Failed to unmount SMB share at " + privateMount, e, Activator.PLUGIN_ID);
		}
		
		privateMount = null;
	}
}
