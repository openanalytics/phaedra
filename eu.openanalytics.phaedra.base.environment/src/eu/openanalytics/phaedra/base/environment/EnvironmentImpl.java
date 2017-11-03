package eu.openanalytics.phaedra.base.environment;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import javax.persistence.EntityManager;

import eu.openanalytics.phaedra.base.db.Database;
import eu.openanalytics.phaedra.base.environment.config.Config;
import eu.openanalytics.phaedra.base.fs.SecureFileServer;
import eu.openanalytics.phaedra.base.security.AuthenticationException;
import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.base.security.ldap.LDAPConfig;

public class EnvironmentImpl implements IEnvironment {

	private String name;
	private Config config;
	
	private SecureFileServer fileServer;
	private Database database;
	
	public EnvironmentImpl(String name, Config config) {
		this.name = name;
		this.config = config;
	}
	
	@Override
	public String getName() {
		return name;
	}
	
	@Override
	public boolean requiresAuthentication() {
		return config.hasCategory(name, "auth");
	}
	
	@Override
	public void connect(String userName, byte[] password) throws AuthenticationException, IOException {
		// Set up security configuration.
		LDAPConfig ldapConfig = null;
		if (requiresAuthentication()) {
			ldapConfig = new LDAPConfig();
			ldapConfig.ldapUrl = config.getValue(name, "auth", "url");
			ldapConfig.defaultDomain = config.getValue(name, "auth", "default.domain");
			ldapConfig.groupPrefix = config.getValue(name, "auth", "group.prefix");
			ldapConfig.groupFilter = config.getValue(name, "auth", "group.filter");
			ldapConfig.principalMapping = config.getValue(name, "auth", "principal.mapping");
			
		}
		SecurityService.createInstance(ldapConfig);
		
		// Perform authentication, if needed.
		SecurityService.getInstance().getLoginHandler().authenticate(userName, password);
		
		// Set up file server connection.
		String fsPath = config.getValue(name, "fs", "path");
		String fsUser = config.getValue(name, "fs", "user");
		String fsPassword = config.resolvePassword(name, "fs");
		fileServer = new SecureFileServer(fsPath, fsUser, fsPassword);

		// Set up database connectivity.
		String dbUrl = config.getValue(name, "db", "url");
		String dbUser = config.getValue(name, "db", "user");
		String dbPassword = config.resolvePassword(name, "db");
		database = new Database(dbUrl, dbUser, dbPassword);
	}

	@Override
	public void disconnect() {
		database.close();
		fileServer.close();
	}

	@Override
	public SecureFileServer getFileServer() {
		return fileServer;
	}

	@Override
	public EntityManager getEntityManager() {
		return database.getEntityManager();
	}

	@Override
	public Connection getJDBCConnection() {
		try {
			return database.getConnectionPoolManager().getConnection();
		} catch (SQLException e) {
			throw new RuntimeException("Failed to obtain a database connection", e);
		}
	}

	@Override
	public String getSetting(String name) {
		return config.getValue(name);
	}
	
	@Override
	public String resolvePassword(String id) throws IOException {
		return config.resolvePassword(id);
	}
}
