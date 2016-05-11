package eu.openanalytics.phaedra.base.util.encrypt;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MD5Checksum {

	public static byte[] create(byte[] contents) {
		try {
			MessageDigest complete = MessageDigest.getInstance("MD5");
			complete.update(contents);
			return complete.digest();
		} catch (NoSuchAlgorithmException e) {
			// Since we control the JRE running Phaedra, this cannot happen.
		}
		return null;
	}

	public static byte[] create(String[] strings) {
		try {
			MessageDigest complete = MessageDigest.getInstance("MD5");
			for (String string: strings) {
				complete.update(string.getBytes());	
			}
			return complete.digest();
		} catch (NoSuchAlgorithmException e) {
			// Since we control the JRE running Phaedra, this cannot happen.
		}
		return null;
	}
	
	public static boolean match(byte[] c1, byte[] c2) {
		if (c1 == null && c2 != null) return false;
		if (c1 != null && c2 == null) return false;
		if (c1 == null && c2 == null) return true;
		if (c1.length != c2.length) return false;
		for (int i=0; i<c1.length; i++) {
			if (c1[i] != c2[i]) return false;
		}
		return true;
	}
}
