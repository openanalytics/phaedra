package eu.openanalytics.phaedra.base.db.pool;

import java.sql.Connection;
import java.sql.SQLException;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import eu.openanalytics.phaedra.base.db.Activator;
import eu.openanalytics.phaedra.base.db.DatabaseConfig;
import eu.openanalytics.phaedra.base.db.JDBCUtils;
import eu.openanalytics.phaedra.base.db.prefs.Prefs;
import eu.openanalytics.phaedra.base.util.threading.ThreadUtils;

public class ConnectionPoolManager implements AutoCloseable {

	private HikariDataSource dataSource;
	private DatabaseConfig cfg;

	public ConnectionPoolManager(DatabaseConfig cfg) {
		this.cfg = cfg;

		Activator.getDefault().getPreferenceStore().addPropertyChangeListener(e -> {
			if (dataSource != null) {
				String property = e.getProperty();
				if (property.equals(Prefs.DB_POOL_SIZE)) {
					Object value = e.getNewValue();
					if (value instanceof Integer) {
						int nrOfConnections = (int) value;
						dataSource.setMaximumPoolSize(nrOfConnections);
						ThreadUtils.configureDBThreadPool(nrOfConnections);
					}
				} else if (property.equals(Prefs.DB_TIME_OUT)) {
					Object value = e.getNewValue();
					if (value instanceof Integer) {
						int timeout = (int) value;
						dataSource.setConnectionTimeout(timeout);
					}
				}
			}
		});
	}

	public void startup() throws SQLException {
		int nrOfDBConnections = Activator.getDefault().getPreferenceStore().getInt(Prefs.DB_POOL_SIZE);
		int dbConnectionTimeOut = Activator.getDefault().getPreferenceStore().getInt(Prefs.DB_TIME_OUT);

		HikariConfig config = new HikariConfig();
		config.setAutoCommit(false);
		config.setMaximumPoolSize(nrOfDBConnections);
		config.setConnectionTimeout(dbConnectionTimeOut);
		config.setJdbcUrl(cfg.get(DatabaseConfig.URL));
		
		String user = cfg.get(DatabaseConfig.USERNAME);
		if (user != null) {
			config.addDataSourceProperty("user", user);
			config.setUsername(user);
		}
		
		String pw = cfg.getEncrypted(DatabaseConfig.PASSWORD);
		if (pw != null) {
			config.addDataSourceProperty("password", pw);
			config.setPassword(pw);
		}

		String[] extraProps = cfg.getKeys(DatabaseConfig.CONN_PARAM_PREFIX);
		for (String p: extraProps) {
			String name = p.substring(DatabaseConfig.CONN_PARAM_PREFIX.length());
			String value = cfg.get(p);
			config.addDataSourceProperty(name, value);
		}
		
		JDBCUtils.customizeHikariSettings(config);
		dataSource = new HikariDataSource(config);
		ThreadUtils.configureDBThreadPool(nrOfDBConnections);
	}

	@Override
	public void close() throws Exception {
		dataSource.close();
	}

	public Connection getConnection() throws SQLException {
		return dataSource.getConnection();
	}

}
