package eu.openanalytics.phaedra.base.util.encrypt;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import eu.openanalytics.phaedra.base.util.io.StreamUtils;

public class AESEncryptor {

	private final static byte[] PRIVATE_KEY = new byte[]{-82, -82, 21, -68, 116, -29, -43, 43, 120, -21, -103, -14, -120, 22, -12, 57};
	
	/*
	 * Encryption & decryption methods.
	 */
	
	public static byte[] encrypt(String plainText) throws GeneralSecurityException {
		SecretKeySpec skeySpec = new SecretKeySpec(PRIVATE_KEY, "AES");
		Cipher cipher = Cipher.getInstance("AES");
		cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
		try {
			byte[] encrypted = cipher.doFinal(plainText.getBytes("UTF-8"));
			return encrypted;
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException("Encryption failed", e);
		}
	}
	
	public static String decrypt(byte[] encrypted) throws GeneralSecurityException {
		SecretKeySpec skeySpec = new SecretKeySpec(PRIVATE_KEY, "AES");
		Cipher cipher = Cipher.getInstance("AES");
		cipher.init(Cipher.DECRYPT_MODE, skeySpec);
		byte[] original = cipher.doFinal(encrypted);
		try {
			String originalString = new String(original, "UTF-8");
			return originalString;
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException("Decryption failed", e);
		}
	}
	
	/*
	 * Key generation methods.
	 */
	
	public static void generateKeyToFile(String path) throws Exception {
		byte[] bytes = generateKey();
		InputStream in = new ByteArrayInputStream(bytes);
		File storageFile = new File(path);
		OutputStream out = new FileOutputStream(storageFile);
		StreamUtils.copyAndClose(in, out);
	}
	
	public static byte[] readKeyFromFile(String path) throws Exception {
		ByteArrayOutputStream bytes =  new ByteArrayOutputStream();
		InputStream in = new FileInputStream(path);
		StreamUtils.copyAndClose(in, bytes);
		return bytes.toByteArray();
	}
	
	public static byte[] generateKey() throws GeneralSecurityException {
		KeyGenerator kgen = KeyGenerator.getInstance("AES");
		SecretKey skey = kgen.generateKey();
		return skey.getEncoded();
	}
	
	/*
	 * Utility & testing
	 */
	
	public static String asHex (byte bytes[]) {
		StringBuffer strbuf = new StringBuffer(bytes.length * 2);
		int i;
		for (i = 0; i < bytes.length; i++) {
			if (((int) bytes[i] & 0xff) < 0x10) strbuf.append("0");
			strbuf.append(Long.toString((int) bytes[i] & 0xff, 16));
		}
		return strbuf.toString();
	}
	
	public static void main(String[] args) {
		try  {
			String myString = "My^Pàsswoª²rd";
			System.out.println("Original: " + myString);
			long startTime = System.currentTimeMillis();
			byte[] encrypted = encrypt(myString);
			long duration = (System.currentTimeMillis() - startTime);
			System.out.println("Encrypted (in " + duration + "ms): " + asHex(encrypted));
			startTime = System.currentTimeMillis();
			String decrypted = decrypt(encrypted);
			duration = (System.currentTimeMillis() - startTime);
			System.out.println("Decrypted (in " + duration + "ms): " + decrypted);
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
}
