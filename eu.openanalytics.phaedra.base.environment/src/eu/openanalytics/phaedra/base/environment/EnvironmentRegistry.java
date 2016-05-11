package eu.openanalytics.phaedra.base.environment;

import java.util.HashMap;
import java.util.Map;

import eu.openanalytics.phaedra.base.environment.config.Config;
import eu.openanalytics.phaedra.base.environment.config.ConfigLoader;

public class EnvironmentRegistry {

	private static EnvironmentRegistry instance = new EnvironmentRegistry();
	
	private Config config;
	private Map<String, IEnvironment> environments;
	
	private EnvironmentRegistry() {
		// Hidden constructor
		config = ConfigLoader.loadConfig();
		environments = new HashMap<>();
		for (String env: config.getEnvironments()) {
			environments.put(env, new EnvironmentImpl(env, config));
		}
	}
	
	public static EnvironmentRegistry getInstance() {
		return instance;
	}
	
	public String[] getEnvironmentNames() {
		return config.getEnvironments();
	}
	
	public IEnvironment getEnvironment(String name) {
		return environments.get(name);
	}
}
