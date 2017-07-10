package eu.openanalytics.phaedra.base.environment;

import java.io.IOException;

import eu.openanalytics.phaedra.base.environment.bootstrap.BootstrapException;
import eu.openanalytics.phaedra.base.environment.bootstrap.BootstrapManager;
import eu.openanalytics.phaedra.base.hook.HookService;
import eu.openanalytics.phaedra.base.hook.IHookArguments;
import eu.openanalytics.phaedra.base.hook.PreHookException;
import eu.openanalytics.phaedra.base.scripting.api.ScriptService;
import eu.openanalytics.phaedra.base.security.AuthenticationException;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;

public class Screening {

	private static final String HOOK_POINT_ID = Activator.PLUGIN_ID + ".loginHookPoint";
	
	private static IEnvironment activeEnvironment;
	
	public static IEnvironment getEnvironment() {
		return activeEnvironment;
	}
	
	/**
	 * Attempt to log in to the specified environment using the supplied credentials.
	 * 
	 * @param env The environment to log in to.
	 * @param userName The name of the user that is logging in. This may be a qualified login name.
	 * @param password The password of the user that is logging in.
	 * @throws AuthenticationException If the login fails for any reason.
	 */
	public static void login(IEnvironment env, String userName, byte[] password) throws AuthenticationException, IOException {
		if (activeEnvironment != null) throw new AuthenticationException("Cannot log in: an environment is already loaded");

		LoginHookArguments args = new LoginHookArguments(userName, env);
		try {
			HookService.getInstance().runPre(HOOK_POINT_ID, args);
		} catch (PreHookException e) {
			throw new AuthenticationException("Pre-login hook failed: " + e.getMessage(), e.getCause());
		}
		
		env.connect(userName, password);
		
		// Check if the environment is blank, and needs bootstrapping.
		try {
			BootstrapManager.bootstrap(env);
		} catch (BootstrapException e) {
			throw new IOException("Failed to bootstrap the environment", e);
		}
		
		// If no exception occurred at this point, the authentication is accepted.
		activeEnvironment = env;
		
		// Run all post-login actions (that may require an active environment set).
		ScriptService.createInstance(env.getFileServer());
		HookService.getInstance().runPost(HOOK_POINT_ID, args);
		EclipseLog.info("User " + userName + " logged in to " + env.getName(), Activator.getDefault());
	}
	
	public static class LoginHookArguments implements IHookArguments {
		
		public String userName;
		public IEnvironment environment;
		
		public LoginHookArguments(String userName, IEnvironment environment) {
			this.userName = userName;
			this.environment = environment;
		}
		
	}
}
