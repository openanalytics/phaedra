package eu.openanalytics.phaedra.app.headless.authenticate;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Properties;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;

import eu.openanalytics.phaedra.app.headless.Activator;
import eu.openanalytics.phaedra.base.environment.EnvironmentRegistry;
import eu.openanalytics.phaedra.base.environment.IEnvironment;
import eu.openanalytics.phaedra.base.environment.Screening;
import eu.openanalytics.phaedra.base.security.AuthenticationException;
import eu.openanalytics.phaedra.base.util.encrypt.AESEncryptor;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;

public class Authentication {

	public static boolean authenticate() throws GeneralSecurityException {
		Properties configFile = Activator.getDefault().getHeadlessProperties();
	
		String username = configFile.getProperty("username");
		String hexpw = configFile.getProperty("password");
		byte[] password = {};
		
		if (hexpw != null && !hexpw.isEmpty()) {
			password = new HexBinaryAdapter().unmarshal(hexpw);
			password = AESEncryptor.decrypt(password).getBytes();
		}
		
		if (username != null && password != null) {
			String envId = configFile.getProperty("environment");
			IEnvironment env = EnvironmentRegistry.getInstance().getEnvironment(envId);
			try {
				Screening.login(env, username, password);
			} catch (AuthenticationException | IOException e) {
				// Login failed. Set up for a retry.
				EclipseLog.error("Login failed: " + e.getMessage(), e.getCause(), Activator.getDefault());
				return false;
			}
		}
		
		return true;
	}
}
