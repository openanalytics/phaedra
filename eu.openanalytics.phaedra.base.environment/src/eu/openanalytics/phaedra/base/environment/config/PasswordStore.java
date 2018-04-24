package eu.openanalytics.phaedra.base.environment.config;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;

import eu.openanalytics.phaedra.base.util.encrypt.AESEncryptor;
import eu.openanalytics.phaedra.base.util.io.StreamUtils;

/**
 * Used to load and store encrypted passwords on the file server.
 */
public class PasswordStore {

	private String pwdPath;
	
	public PasswordStore(String pwdPath) {
		this.pwdPath = pwdPath;
	}
	
	/**
	 * Use this method to load the password from the file server.
	 * 
	 * @param entry the name of the password file (extension = .AES)
	 * @return the password
	 * @throws IOException when accessing the file server fails.
	 * @throws GeneralSecurityException
	 */
	public String getPassword(String entry) throws IOException {
		if (pwdPath == null) throw new IOException("Remote password retrieval is disabled for this environment");
		
		String filePath = pwdPath + "/" + entry + ".aes";
		InputStream in = ConfigLoader.openStream(filePath);
		
		byte[] encrypted = StreamUtils.readAll(in);
		if (encrypted != null) {
			try {
				return AESEncryptor.decrypt(encrypted);
			} catch (GeneralSecurityException e) {
				throw new IOException("Failed to decrypt password", e);
			}
		}
		
		return null;
	}
	
	/**
	 * Save the password to the file server.
	 * Note: does not support SMB paths.
	 * 
	 * @param entry the name of the password file (without .AES)
	 * @param password the password that must be encrypted and saved in the password file.
	 * @throws IOException If the encryption fails.
	 */
	public void savePassword(String entry, String password) throws IOException {
		if (pwdPath == null) throw new IOException("Password storage is disabled for this store");
		String filePath = pwdPath + File.separator + entry + ".aes";
		encrypt(password, filePath);
	}
	
	/**
	 * Encrypt a password and write the encrypted bytes to the output stream.
	 * 
	 * @param password The password to encrypt.
	 * @param destination The destination file where the encrypted bytes will be written to.
	 * @throws IOException If the encryption fails.
	 */
	public void encrypt(String password, String destination) throws IOException {
		byte[] encrypted;
		new File(destination).getParentFile().mkdirs();
		try (OutputStream out = new FileOutputStream(destination)) {
			encrypted = AESEncryptor.encrypt(password);
			StreamUtils.copyAndClose(new ByteArrayInputStream(encrypted), out);
		} catch (GeneralSecurityException e) {
			throw new IOException("Failed to encrypt password", e);
		}
	}
}
