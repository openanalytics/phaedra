package eu.openanalytics.phaedra.base.bootstrap.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import eu.openanalytics.phaedra.base.environment.IEnvironment;
import eu.openanalytics.phaedra.base.environment.bootstrap.BootstrapException;
import eu.openanalytics.phaedra.base.environment.bootstrap.IBootstrapper;

public class DatabaseBootstrapper implements IBootstrapper {

	@Override
	public void bootstrap(IEnvironment env) throws BootstrapException {
		try (Connection conn = env.getJDBCConnection()) {
			// Execute the bootstrap script.
			try (Statement stmt = conn.createStatement()) {
				stmt.execute("RUNSCRIPT FROM 'classpath:eu/openanalytics/phaedra/base/bootstrap/db/setup.sql'");
			}
		} catch (SQLException e) {
			throw new BootstrapException("Failed to bootstrap database", e);
		}
	}

}
