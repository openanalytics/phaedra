package eu.openanalytics.phaedra.base.environment;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import eu.openanalytics.phaedra.base.environment.config.Config;
import eu.openanalytics.phaedra.base.environment.config.ConfigLoader;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;

public class EnvironmentRegistry {

	private static EnvironmentRegistry instance = new EnvironmentRegistry();
	
	private String[] environmentNames;
	private Map<String, IEnvironment> environments;
	
	private EnvironmentRegistry() {
		// Hidden constructor
		environmentNames = new String[0];
		environments = new HashMap<>();
		try {
			Config config = ConfigLoader.loadConfig();
			Arrays.stream(config.getEnvironments()).map(e -> new EnvironmentImpl(e, config)).forEach(e -> environments.put(e.getName(), e));
			environmentNames = config.getEnvironments();
		} catch (Throwable t) {
			EclipseLog.error("Failed to load environment configuration", t, Activator.getDefault());
		}
	}
	
	public static EnvironmentRegistry getInstance() {
		return instance;
	}
	
	public String[] getEnvironmentNames() {
		return environmentNames;
	}
	
	public IEnvironment getEnvironment(String name) {
		return environments.get(name);
	}
}
