package eu.openanalytics.phaedra.base.db.jpa;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import org.eclipse.persistence.config.SessionCustomizer;
import org.eclipse.persistence.exceptions.DatabaseException;
import org.eclipse.persistence.sessions.DefaultConnector;
import org.eclipse.persistence.sessions.Session;

import eu.openanalytics.phaedra.base.db.pool.ConnectionPoolManager;

/**
 * Customize EclipseLink sessions so that they use our own connection pooling
 * mechanism provided by {@link ConnectionPoolManager}.
 */
public class JPASessionCustomizer implements SessionCustomizer {

	public final static String PROP_CONNECTION_POOL = "ConnectionPoolManager";
	
	@Override
	public void customize(Session session) throws Exception {
		session.getLogin().setConnector(new DefaultConnector() {
			private static final long serialVersionUID = -6647243600944161685L;
			@Override
			public Connection connect(Properties props, Session session) throws DatabaseException {
				try {
					ConnectionPoolManager cpm = (ConnectionPoolManager) session.getProperty(PROP_CONNECTION_POOL);
					return cpm.getConnection();
				} catch (SQLException e) {
					throw DatabaseException.sqlException(e);
				}
			}
		});
	}

}
