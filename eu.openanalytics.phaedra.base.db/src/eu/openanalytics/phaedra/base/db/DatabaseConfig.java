package eu.openanalytics.phaedra.base.db;

import eu.openanalytics.phaedra.base.util.misc.ConfigResolver;

public class DatabaseConfig extends ConfigResolver {

	public static final String URL = "url";
	public static final String USERNAME = "user";
	public static final String PASSWORD = "password";
	
	public static final String CONN_PARAM_PREFIX = "connection.parameter.";
	
	public static final String JPA_SESSION_EM = "jpa.session.entity.manager";
	public static final String JPA_LOG_LEVEL = "jpa.logging";
	public static final String JPA_CACHE_L2_ENABLED = "jpa.cache.l2.enabled";
	
	public DatabaseConfig() {
		super("phaedra.db.");
	}
}
