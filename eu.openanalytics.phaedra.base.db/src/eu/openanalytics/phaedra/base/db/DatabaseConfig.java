package eu.openanalytics.phaedra.base.db;

import eu.openanalytics.phaedra.base.util.misc.ConfigResolver;

public class DatabaseConfig extends ConfigResolver {

	public static final String URL = "url";
	public static final String USERNAME = "user";
	public static final String PASSWORD = "password";
	
	public static final String CONN_PARAM_PREFIX = "connection.parameter.";
	
	public static final String JPA_LOG_LEVEL = "jpa.logging";
	
	public DatabaseConfig() {
		super("phaedra.db.");
	}
}
