package eu.openanalytics.phaedra.base.environment;

import java.io.IOException;
import java.sql.Connection;
import java.util.Collection;

import javax.persistence.EntityManager;

import eu.openanalytics.phaedra.base.environment.config.Config;
import eu.openanalytics.phaedra.base.fs.SecureFileServer;
import eu.openanalytics.phaedra.base.security.AuthenticationException;

/**
 * An IEnvironment provides the platform with sources to obtain and manipulate data in.
 * This includes:
 * <ul>
 * <li>A database, accessible via an EntityManager or a direct JDBC connection</li>
 * <li>A file server, accessible via a SecureFileServer</li>
 * </ul>
 */
public interface IEnvironment {

	public String getName();
	
	/**
	 * Returns if authentication is required for this environment.
	 */
	boolean requiresAuthentication();
	/**
	 * Returns the parameters required for connect.
	 **/
	Collection<String> getRequiredConnectParameters();
	
	public void connect(String userName, byte[] password) throws AuthenticationException, IOException;
	public void disconnect();
	
	public SecureFileServer getFileServer();
	
	@Deprecated
	public EntityManager getEntityManager();
	
	public Connection getJDBCConnection();
	
	public Config getConfig();
}
