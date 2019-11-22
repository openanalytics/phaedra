package eu.openanalytics.phaedra.base.environment;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;

import javax.persistence.EntityManager;

import eu.openanalytics.phaedra.base.db.Database;
import eu.openanalytics.phaedra.base.db.DatabaseConfig;
import eu.openanalytics.phaedra.base.environment.config.Config;
import eu.openanalytics.phaedra.base.fs.FileServerConfig;
import eu.openanalytics.phaedra.base.fs.SecureFileServer;
import eu.openanalytics.phaedra.base.security.AuthConfig;
import eu.openanalytics.phaedra.base.security.AuthenticationException;
import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.base.security.model.Permissions;

public class EnvironmentImpl implements IEnvironment {

	private String name;
	private Config config;
	
	private SecurityService securityService;
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
	
	private synchronized SecurityService getSecurityService() {
		SecurityService service = this.securityService;
		if (service == null) {
			service = createSecurityService();
			this.securityService = service;
		}
		return service;
	}
	
	private SecurityService createSecurityService() {
		AuthConfig authConfig = null;
		if (requiresAuthentication()) {
			authConfig = new AuthConfig();
			authConfig.setResolver(key -> config.getValue(name, "auth", key));
		}
		Permissions permissions = new Permissions();
		permissions.load(key -> config.getValue(name, "permissions", key));
		
		return new SecurityService(authConfig, permissions);
	}
	
	@Override
	public Collection<String> getRequiredConnectParameters() {
		final SecurityService securityService = getSecurityService();
		return securityService.getLoginHandler().getRequiredParameter();
	}
	
	@Override
	public void connect(String userName, byte[] password) throws AuthenticationException, IOException {
		final SecurityService securityService = getSecurityService();
		securityService.login(userName, password);
		
		FileServerConfig fsConfig = new FileServerConfig();
		fsConfig.setResolver(key -> config.getValue(name, "fs", key)); 
		fsConfig.setEncryptedResolver(key -> {
			if (key == null || key.equalsIgnoreCase("password")) return config.resolvePassword(name, "fs");
			else return config.resolvePassword(key);
		});
		fsConfig.setKeySupplier(prefix -> config.getKeys(name, "fs", prefix));
		fileServer = new SecureFileServer(fsConfig);

		DatabaseConfig dbConfig = new DatabaseConfig();
		dbConfig.setResolver(key -> config.getValue(name, "db", key)); 
		dbConfig.setEncryptedResolver(key -> {
			if (key == null || key.equalsIgnoreCase("password")) return config.resolvePassword(name, "db");
			else return config.resolvePassword(key);
		});
		dbConfig.setKeySupplier(prefix -> config.getKeys(name, "db", prefix));
		database = new Database(dbConfig);
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
	public Config getConfig() {
		return config;
	}
}
