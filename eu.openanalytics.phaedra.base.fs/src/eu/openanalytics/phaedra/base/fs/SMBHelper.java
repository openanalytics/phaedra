package eu.openanalytics.phaedra.base.fs;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import eu.openanalytics.phaedra.base.util.process.ProcessUtils;
import jcifs.CIFSContext;
import jcifs.context.SingletonContext;

public class SMBHelper {

	public static final String SMB_PROTOCOL_PREFIX = "smb://";
	public static final String UNC_PROTOCOL_PREFIX = "\\\\";
	
	private static final CIFSContext GUEST_CTX = SingletonContext.getInstance().withGuestCrendentials();
	
	public static boolean isSMBPath(String path) {
		return path != null && (path.startsWith(SMB_PROTOCOL_PREFIX) || path.startsWith(UNC_PROTOCOL_PREFIX));
	}
	
	public static InputStream open(String path) throws IOException {
		// Access the file without authentication: on Windows, let the OS attempt instead of jcifs.
		if (ProcessUtils.isWindows()) {
			return new FileInputStream(toUNCNotation(path));
		} else {
			return GUEST_CTX.get(path).openInputStream();
		}
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
