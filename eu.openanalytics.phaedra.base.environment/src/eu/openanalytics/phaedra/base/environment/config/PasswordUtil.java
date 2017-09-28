package eu.openanalytics.phaedra.base.environment.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;

public class PasswordUtil implements IApplication {

	@Override
	public Object start(IApplicationContext context) throws Exception {
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		
		System.out.println("Password utility: encrypt passwords for PWD storage");
		System.out.print("Enter the id of the account:" );
		String id = reader.readLine();
		System.out.print("Enter the password of the account: ");
		String password = reader.readLine();
		
		String destination = new File(String.format("./%s.aes", id)).getCanonicalPath();
		new PasswordStore(null).encrypt(password, destination);
		System.out.println("The encrypted password has been saved at: " + destination);
		System.out.println("Please copy this file into your PWD folder.");
		return IApplication.EXIT_OK;
	}

	@Override
	public void stop() {
		// Nothing to do.
	}

}
