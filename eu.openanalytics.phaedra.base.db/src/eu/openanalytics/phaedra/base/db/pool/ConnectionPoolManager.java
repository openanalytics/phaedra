package eu.openanalytics.phaedra.base.db.pool;

import java.security.GeneralSecurityException;
import java.sql.Connection;
import java.sql.SQLException;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import eu.openanalytics.phaedra.base.db.Activator;
import eu.openanalytics.phaedra.base.db.JDBCUtils;
import eu.openanalytics.phaedra.base.db.prefs.Prefs;
import eu.openanalytics.phaedra.base.util.encrypt.AESEncryptor;
import eu.openanalytics.phaedra.base.util.threading.ThreadUtils;

public class ConnectionPoolManager implements AutoCloseable {

	private HikariDataSource dataSource;

	private String url;
	private String user;
	private byte[] pw;

	public ConnectionPoolManager(String url, String user, String pw) {
		this.url = url;
		this.user = user;
		try {
			if (pw != null) this.pw = AESEncryptor.encrypt(pw);
		} catch (GeneralSecurityException e) {
			throw new RuntimeException("Encryption failed", e);
		}

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
		try {
			int nrOfDBConnections = Activator.getDefault().getPreferenceStore().getInt(Prefs.DB_POOL_SIZE);
			int dbConnectionTimeOut = Activator.getDefault().getPreferenceStore().getInt(Prefs.DB_TIME_OUT);

			HikariConfig config = new HikariConfig();
			config.setAutoCommit(false);
			config.setMaximumPoolSize(nrOfDBConnections);
			config.setConnectionTimeout(dbConnectionTimeOut);
			config.setJdbcUrl(url);
			if (user != null) {
				config.addDataSourceProperty("user", user);
				config.setUsername(user);
			}
			if (pw != null) {
				config.addDataSourceProperty("password", AESEncryptor.decrypt(pw));
				config.setPassword(AESEncryptor.decrypt(pw));
			}

			JDBCUtils.customizeHikariSettings(config);
			dataSource = new HikariDataSource(config);
			ThreadUtils.startDBPool(nrOfDBConnections);
		} catch (GeneralSecurityException e) {
			throw new RuntimeException("Decryption failed", e);
		}
	}

	@Override
	public void close() throws Exception {
		dataSource.close();
	}

	public Connection getConnection() throws SQLException {
		return dataSource.getConnection();
	}

}
