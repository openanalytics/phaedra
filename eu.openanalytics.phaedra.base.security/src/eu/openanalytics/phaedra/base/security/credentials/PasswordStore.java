package eu.openanalytics.phaedra.base.security.credentials;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.security.GeneralSecurityException;

import eu.openanalytics.phaedra.base.util.encrypt.AESEncryptor;
import eu.openanalytics.phaedra.base.util.io.StreamUtils;

/**
 * Class used to load and store passwords (encrypted) on the file server.
 * 
 * Also has a commandline interface to manage the password store.
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
		String filePath = pwdPath + File.separator + entry + ".aes";
		InputStream in = new FileInputStream(filePath);
		if (in != null) {
			byte[] encrypted = StreamUtils.readAll(in);
			if (encrypted != null) {
				try {
					return AESEncryptor.decrypt(encrypted);
				} catch (GeneralSecurityException e) {
					throw new IOException("Failed to decrypt password", e);
				}
			}
		}
		return null;
	}
	
	/**
	 * Save the password to the file server.
	 * 
	 * @param entry the name of the password file (without .AES)
	 * @param password the password that must be encrypted and saved in the password file.
	 * @throws IOException
	 * @throws GeneralSecurityException
	 */
	public void savePassword(String entry, String password) throws IOException {
		String filePath = pwdPath + File.separator + entry + ".aes";
		File f = new File(filePath);
		f.getParentFile().mkdirs();
		OutputStream out = new FileOutputStream(f);
		byte[] encrypted;
		try {
			encrypted = AESEncryptor.encrypt(password);
		} catch (GeneralSecurityException e) {
			out.close();
			throw new IOException("Failed to encrypt password", e);
		}
		InputStream in = new ByteArrayInputStream(encrypted);
		StreamUtils.copyAndClose(in, out);
	}
	
	/**
	 * Test application to read and store passwords.
	 * 
	 * @param args (not used)
	 */
	public static void main(String[] args) {
		InputStreamReader isr = new InputStreamReader(System.in);
		BufferedReader r = new BufferedReader(isr);

		try {
			System.out.print("Enter the path to the password storage directory: ");
			String cmd = r.readLine();
			System.out.println("Using directory: " + cmd);
			PasswordStore store = new PasswordStore(cmd);
			
			while (!cmd.equals("x")) {
				System.out.print("Enter (1) to look up a password, (2) to save a password, (x) to exit: ");
				cmd = r.readLine();
				if (cmd.equals("1")) {
					System.out.print("Enter the account name: ");
					String ds = r.readLine();
					try {
						String password = store.getPassword(ds);
						if (password != null) {
							System.out.println("Account "+ ds + " has password " + password);
						} else {
							System.out.println("No password configured for account " + ds);
						}
					} catch (Exception e) {
						System.out.println("Failed to look up password for account " + ds + ":");
						e.printStackTrace();
					}
		
				} else if (cmd.equals("2")) {
					System.out.print("Enter the account name: ");
					String ds = r.readLine();
					System.out.print("Enter the new password: ");
					String newPw = r.readLine();
					try {
						store.savePassword(ds, newPw);
						System.out.println("New password saved.");
					} catch (Exception e) {
						System.out.println("Failed to save the new password:");
						e.printStackTrace();
					}
				} else if (cmd.equals("x")) {
					System.exit(0);
				} else {
					System.out.println("Unknown command: " + cmd);
				}
			}
		} catch (IOException e) {}
	}
}
