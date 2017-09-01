package eu.openanalytics.phaedra.base.fs;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbFile;

public class SMBHelper {

	public static final String SMB_PROTOCOL_PREFIX = "smb://";
	
	public static boolean isSMBPath(String path) {
		return path != null && (path.startsWith(SMB_PROTOCOL_PREFIX) || path.startsWith(SecureFileServer.UNC_PREFIX));
	}
	
	public static InputStream open(String smbPath) throws IOException {
		SmbFile file = getFile(smbPath, null, false);
		return file.getInputStream();
	}
	
	public static SmbFile getFile(String smbPath, NtlmPasswordAuthentication auth, boolean lock) throws MalformedURLException {
		int sharing = SmbFile.FILE_SHARE_READ;
		if (!lock) sharing = sharing | SmbFile.FILE_SHARE_WRITE | SmbFile.FILE_SHARE_DELETE;
		
		if (smbPath.startsWith(SecureFileServer.UNC_PREFIX)) {
			smbPath = smbPath.substring(SecureFileServer.UNC_PREFIX.length());
		}
		
		if (!smbPath.startsWith(SMB_PROTOCOL_PREFIX)) {
			smbPath = SMB_PROTOCOL_PREFIX + smbPath;
		}
		
		smbPath = smbPath.replace('\\', '/');
		
		SmbFile sFile = new SmbFile(smbPath, auth, sharing);
		return sFile;
	}
}
