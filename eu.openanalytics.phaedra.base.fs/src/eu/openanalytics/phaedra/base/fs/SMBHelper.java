package eu.openanalytics.phaedra.base.fs;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;

import eu.openanalytics.phaedra.base.util.process.ProcessUtils;
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbFile;

public class SMBHelper {

	public static final String SMB_PROTOCOL_PREFIX = "smb://";
	public final static String UNC_PROTOCOL_PREFIX = "\\\\";
	
	public static boolean isSMBPath(String path) {
		return path != null && (path.startsWith(SMB_PROTOCOL_PREFIX) || path.startsWith(UNC_PROTOCOL_PREFIX));
	}
	
	public static InputStream open(String path) throws IOException {
		// Access the file without authentication: on Windows, let the OS attempt instead of jcifs.
		if (ProcessUtils.isWindows()) {
			return new FileInputStream(toUNCNotation(path));
		} else {
			SmbFile file = getFile(path, null, false);
			return file.getInputStream();
		}
	}
	
	public static SmbFile getFile(String path, NtlmPasswordAuthentication auth, boolean lock) throws MalformedURLException {
		int sharing = SmbFile.FILE_SHARE_READ;
		if (!lock) sharing = sharing | SmbFile.FILE_SHARE_WRITE | SmbFile.FILE_SHARE_DELETE;
		
		return new SmbFile(toSMBNotation(path), auth, sharing);
	}
	
	public static String toUNCNotation(String smbPath) {
		if (smbPath.startsWith(SMB_PROTOCOL_PREFIX)) {
			smbPath = smbPath.substring(SMB_PROTOCOL_PREFIX.length());
		}
		
		if (!smbPath.startsWith(UNC_PROTOCOL_PREFIX)) {
			smbPath = UNC_PROTOCOL_PREFIX + smbPath;
		}
		
		return smbPath;
	}
	
	public static String toSMBNotation(String smbPath) {
		if (smbPath.startsWith(UNC_PROTOCOL_PREFIX)) {
			smbPath = smbPath.substring(UNC_PROTOCOL_PREFIX.length());
		}
		
		if (!smbPath.startsWith(SMB_PROTOCOL_PREFIX)) {
			smbPath = SMB_PROTOCOL_PREFIX + smbPath;
		}
		
		smbPath = smbPath.replace('\\', '/');
		return smbPath;
	}
}
