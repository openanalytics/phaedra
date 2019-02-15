package eu.openanalytics.phaedra.app.headless.authenticate;

import java.io.IOException;
import java.util.Properties;

import eu.openanalytics.phaedra.app.headless.Activator;
import eu.openanalytics.phaedra.base.environment.EnvironmentRegistry;
import eu.openanalytics.phaedra.base.environment.IEnvironment;
import eu.openanalytics.phaedra.base.environment.Screening;
import eu.openanalytics.phaedra.base.environment.config.ConfigLoader;
import eu.openanalytics.phaedra.base.security.AuthenticationException;
import eu.openanalytics.phaedra.base.security.PermissionDeniedException;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;

public class Authentication {

	private static final String PROP_ENV = "phaedra.env";
	private static final String PROP_USER = "phaedra.username";
	private static final String PROP_PW = "phaedra.password";
	private static final String PROP_PW_ID = "phaedra.pwd-id";
	
	public static boolean authenticate() throws PermissionDeniedException {
		Properties config = Activator.getDefault().getHeadlessProperties();
	
		String username = config.getProperty(PROP_USER);
		String password = config.getProperty(PROP_PW);
		String pwdId = config.getProperty(PROP_PW_ID);
		
		if (pwdId != null && !pwdId.isEmpty()) {
			try {
				password = ConfigLoader.loadConfig().resolvePassword(pwdId);
			} catch (Exception e) {
				throw new IllegalArgumentException(String.format("Failed to retrieve password with id %s", pwdId));
			}
		}
		
		if (username == null || password == null) {
			throw new PermissionDeniedException(String.format("Cannot authenticate: properties %s and %s not set", PROP_USER, PROP_PW));
		}
		
		String envId = config.getProperty(PROP_ENV);
		if (envId == null) throw new IllegalArgumentException(String.format("Cannot start: property %s not set", PROP_ENV));
		
		try {
			EnvironmentRegistry.getInstance().initialize();
			IEnvironment env = EnvironmentRegistry.getInstance().getEnvironment(envId);
			Screening.login(env, username, password.getBytes());
		} catch (AuthenticationException | IOException e) {
			// Login failed. Set up for a retry.
			EclipseLog.error("Login failed: " + e.getMessage(), e.getCause(), Activator.getDefault());
			return false;
		}
		
		return true;
	}
}
