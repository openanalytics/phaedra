package eu.openanalytics.phaedra.base.db;

import java.io.IOException;
import java.io.Writer;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.eclipse.persistence.config.PersistenceUnitProperties;
import org.eclipse.persistence.jpa.JpaEntityManager;
import org.eclipse.persistence.platform.database.H2Platform;
import org.eclipse.persistence.platform.database.PostgreSQLPlatform;
import org.eclipse.persistence.queries.DatabaseQuery;
import org.eclipse.persistence.sessions.Session;
import org.postgresql.util.PGobject;

import com.zaxxer.hikari.HikariConfig;

import eu.openanalytics.phaedra.base.db.jpa.LockingEntityManager;
import eu.openanalytics.phaedra.base.db.jpa.PatchedOraclePlatform;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.base.util.misc.NumberUtils;
import oracle.jdbc.driver.OracleConnection;
import oracle.sql.CLOB;

public class JDBCUtils {

	private enum DbType {
		
		Oracle("oracle.jdbc.OracleDriver"),
		Postgresql("org.postgresql.Driver"),
		H2("org.h2.Driver"),
		MonetDB("nl.cwi.monetdb.jdbc.MonetDriver"),
		Unknown("");

		private String driverClassName;
		
		private DbType(String driverClassName) {
			this.driverClassName = driverClassName;
		}
		
		public String getDriverClassName() {
			return driverClassName;
		}
		
		public static DbType getByName(String name) {
			for (DbType t: values()) {
				if (t.name().equalsIgnoreCase(name)) return t;
			}
			return Unknown;
		}
		
		public static DbType getByURL(String url) {
			String type = url.split(":")[1];
			return getByName(type);
		}
	};
	
	private static DbType dbType;
	
	public static void checkDbType(String url) {
		if (dbType == null) dbType = DbType.getByURL(url);
		if (dbType == DbType.Unknown) throw new RuntimeException("Unsupported database type: " + url);
		loadJDBCDriver(url);
	}
	
	public static void loadJDBCDriver(String url) {
		DbType type = DbType.getByURL(url);
		if (type == DbType.Unknown) return;
		try {
			Class.forName(type.getDriverClassName());
		} catch (ClassNotFoundException e) {
			throw new RuntimeException("Failed to initialize database driver: " + type.getDriverClassName());
		}
	}
	
	public static boolean isOracle() {
		return dbType == DbType.Oracle;
	}

	public static boolean isPostgres() {
		return dbType == DbType.Postgresql;
	}

	public static boolean isEmbedded() {
		return dbType == DbType.H2;
	}
	
	public static void customizeJPASettings(Map<String, Object> properties) {
		properties.put(PersistenceUnitProperties.JDBC_DRIVER, dbType.getDriverClassName());
		if (isOracle()) {
			properties.put(PersistenceUnitProperties.BATCH_WRITING, "Oracle-JDBC");
			properties.put(PersistenceUnitProperties.TARGET_DATABASE, PatchedOraclePlatform.class.getName());
			// Oracle specific properties. Always use BINARY_DOUBLE for setDouble.
			properties.put("SetFloatAndDoubleUseBinary", "true");
		} else if (isEmbedded()) {
			properties.put(PersistenceUnitProperties.TARGET_DATABASE, H2Platform.class.getName());
		} else {
			properties.put(PersistenceUnitProperties.TARGET_DATABASE, PostgreSQLPlatform.class.getName());
		}
	}
	
	public static void customizeHikariSettings(HikariConfig config) {
		config.setDriverClassName(dbType.getDriverClassName());
		// Oracle specific properties.
		if (isOracle()) {
			// Make sure the NLS_NUMERIC_CHARACTERS are set correctly for each new connection in the pool.
			config.setConnectionInitSql("ALTER SESSION SET NLS_NUMERIC_CHARACTERS = '.,'");
			// Always use BINARY_DOUBLE for setDouble.
			config.addDataSourceProperty("SetFloatAndDoubleUseBinary", "true");
		}
	}
	
	/**
	 * (Oracle-specific) Create a CLOB to hold XML data.
	 * 
	 * @param xmlData The XML to hold.
	 * @param conn The target connection that will consume the CLOB.
	 * @return A CLOB object that can be used in statements.
	 * @throws SQLException If the CLOB cannot be created.
	 */
	public static CLOB makeCLOB(String xmlData, OracleConnection conn) throws SQLException {
		CLOB tempClob = CLOB.createTemporary(conn, true, CLOB.DURATION_SESSION);
		tempClob.open(CLOB.MODE_READWRITE);
		Writer tempClobWriter = tempClob.setCharacterStream(0l);
		try {
			tempClobWriter.write(xmlData);
			tempClobWriter.flush();
		} catch (IOException e) {
			throw new SQLException("Failed to generate CLOB", e);
		} finally {
			try {tempClobWriter.close(); } catch (IOException e) {}
			tempClob.close();
		}
		return tempClob;
	}

	/**
	 * <p>Set the content of an XML column in a query. Requires a parameter.</p>
	 *
	 * <p>Should be used in combination with {@link JDBCUtils#getXMLObjectParameter(String)}.</p>
	 *
	 * <p><code>Query query = em.createNativeQuery("update table t set " + JDBCUtils.updateXMLColumn("data_xml") + " where ...");<br/>
	 * query.setParameter(1, JDBCUtils.getXMLObjectParameter(xml));</code></p>
	 *
	 * @param columnName The table column name
	 * @return Database platform independent column update
	 */
	public static String updateXMLColumn(String columnName) {
		return columnName + " = " + (isOracle() ? "XmlType(?)" : "?");
	}

	/**
	 * <p>Retrieve the content from an XML column as a string in a query.</p>
	 *
	 * <p><code>Query sqlQuery = "select " + JDBCUtils.selectXMLColumn("p.data_xml") + " from ..."</code></p>
	 *
	 * @param columnName The table column name
	 * @return Database platform independent column select
	 */
	public static String selectXMLColumn(String columnName) {
		if (isOracle()) {
			return columnName + ".getClobVal() clob";
		}
		return columnName;
	}

	/**
	 * <p>Return the XML String as an XML Object for current database platform.</p>
	 *
	 * <p><code>query.setParameter(1, JDBCUtils.getXMLObjectParameter(xml));</code></p>
	 *
	 * @param xml XML as string
	 * @return
	 */
	public static Object getXMLObjectParameter(String xml) {
		if (isOracle()) {
			// Oracle
			return new XMLTypeHolder(xml);
		} else if (isEmbedded()) {
			// H2
			return xml;
		} else {
			// PostgreSQL
			PGobject o = new PGobject();
			o.setType("xml");
			try {
				o.setValue(xml);
			} catch (SQLException e) {
				EclipseLog.error(e.getMessage(), e, Activator.getDefault());
			}
			return o;
		}
	}

	public static class XMLTypeHolder {
		public String xml;

		public XMLTypeHolder(String xml) {
			this.xml = xml;
		}
	}

	public static Session getSession(EntityManager em) {
		Session session = ((JpaEntityManager)em.getDelegate()).getSession();
		return session;
	}

	public static String getFormatNumberSQL(String selectVal, int decimals) {
		if (isOracle()) {
			return "CASE WHEN " + selectVal + " IS NAN THEN 'NaN' WHEN " + selectVal + " IS INFINITE THEN 'Inf' ELSE TO_CHAR(ROUND(" + selectVal + ", " + decimals + ")) END";
		} else {
			return "CASE WHEN " + selectVal + " = 'NaN' THEN 'NaN' WHEN " + selectVal + " = 'Infinity' THEN 'Inf' ELSE cast(ROUND(cast(" + selectVal + " as numeric), " + decimals + ") as text) END";
		}
	}
	
	public static long getSequenceNextVal(EntityManager em, String sequenceName) {
		String statement = null;
		if (isOracle()) {
			statement = "select " + getSequenceNextValSQL(sequenceName) + " from dual";
		} else {
			statement = "select " + getSequenceNextValSQL(sequenceName);
		}
		Query query = em.createNativeQuery(statement);
		List<?> res = JDBCUtils.queryWithLock(query, em);
		return ((Number)res.get(0)).longValue();
	}
	
	public static String getSequenceNextValSQL(String sequenceName) {
		if (isOracle()) {
			return sequenceName + ".nextval";
		} else {
			return "nextval('" + sequenceName + "')";
		}
	}

	public static boolean testFunctionExists(Connection conn, String namespace, String name) throws SQLException {
		String query = null;
		if (isOracle()) {
			query = "select procedure_name from all_procedures where object_name = '" + namespace.toUpperCase() + "' and procedure_name = '" + name.toUpperCase() + "'";
		} else {
			query = "select routine_name from information_schema.routines where routine_schema = '" + namespace + "' and routine_name = '" + name + "'";
		}
		ResultSet res = conn.createStatement().executeQuery(query);
		return res.next();
	}
	
	public static String getFromDual() {
		if (isOracle()) {
			return " from dual";
		} else {
			return "";
		}
	}
	
	public static String selectConcat(String col1, String col2, char separator) {
		if (isOracle()) {
			return col1 + " ||'" + separator + "'|| " + col2;
		} else {
			return "concat_ws('" + separator + "', " + col1 + ", " + col2 + ")";
		}
	}
	
	public static String getDBSize(Connection conn) throws SQLException {
		StringBuilder response = new StringBuilder();
		if (isEmbedded()) {
			response.append("Embedded database: size n/a");
		} else if (isOracle()) {
			Object[] output = callFunction(conn, "hca_utils_pack.get_tablespace_info", new String[] { "PHAEDRA_D" }, new int[] { Types.VARCHAR }, new int[] { Types.BIGINT, Types.BIGINT });
			long used = (Long) output[0];
			long free = (Long) output[1];
			double pctFull = 100 * ((double) used) / (used + free);
			response.append("Data space: " + NumberUtils.round(pctFull, 2) + "% full ("  + NumberUtils.formatGB(used) + "/" + NumberUtils.formatGB(used + free) + " Gb)\n");
			
			output = callFunction(conn, "hca_utils_pack.get_tablespace_info", new String[] { "PHAEDRA_I" }, new int[] { Types.VARCHAR }, new int[] { Types.BIGINT, Types.BIGINT });
			used = (Long) output[0];
			free = (Long) output[1];
			pctFull = 100 * ((double) used) / (used + free);
			response.append("Index space: " + NumberUtils.round(pctFull, 2) + "% full ("  + NumberUtils.formatGB(used) + "/" + NumberUtils.formatGB(used + free) + " Gb)");
		} else {
			Long retVal = (Long) JDBCUtils.callFunction(conn, "pg_database_size", new Object[] {"phaedra"}, new int[] {Types.VARCHAR}, Types.BIGINT);
			response.append("Used space: " + NumberUtils.formatGB(retVal.longValue()) + " Gb");
		}
		return response.toString();
	}
	
	/**
	 * <p>Call a Stored Procedure/Function.</p>
	 *
	 * <p>The function name should be package.function or schema.function (e.g. hca_utils_pack.get_tablespace_info).</p>
	 *
	 * <p><strong>Connection must be closed by the caller.</strong></p>
	 *
	 * @param conn An open Connection that will be used to call the function.
	 * @param name The name of the function.
	 * @param inArgs The IN arguments.
	 * @param inArgTypes The IN argument SQLTypes. Must be same size as inArgs.
	 * @throws SQLException
	 */
	public static void callFunction(Connection conn, String name, Object[] inArgs, int[] inArgTypes) throws SQLException {
		callFunction(conn, name, inArgs, inArgTypes, new int[0]);
	}

	/**
	 * <p>Call a Stored Procedure/Function that will return values using OUT parameters.</p>
	 *
	 * <p>The function name should be package.function or schema.function (e.g. hca_utils_pack.get_tablespace_info).</p>
	 *
	 * <p><strong>Connection must be closed by the caller.</strong></p>
	 *
	 * @param conn An open Connection that will be used to call the function.
	 * @param name The name of the function.
	 * @param inArgs The IN arguments.
	 * @param inArgTypes The IN argument SQLTypes. Must be same size as inArgs.
	 * @param outArgTypes The OUT argument SQLTypes.
	 * @return The OUT values.
	 * @throws SQLException
	 */
	public static Object[] callFunction(Connection conn, String name, Object[] inArgs, int[] inArgTypes, int[] outArgTypes) throws SQLException {
		int totalArgs = inArgTypes.length + outArgTypes.length;
		String args = IntStream.range(0, totalArgs).mapToObj(i -> "?").collect(Collectors.joining(","));
		try (CallableStatement cs = conn.prepareCall("{ call "+ (isOracle() ? "phaedra." : "") + name + "(" + args + ") }")) {
			int arg = 1;
			// IN arguments.
			for (int i = 0; i < inArgs.length; i++) {
				cs.setObject(arg++, inArgs[i], inArgTypes[i]);
			}
			// OUT arguments.
			for (int i = 0; i < outArgTypes.length; i++) {
				cs.registerOutParameter(arg++, outArgTypes[i]);
			}
			// Execute the function.
			cs.execute();

			// Gather the OUT parameter results.
			Object[] output = new Object[outArgTypes.length];
			for (int i = 0; i < output.length; i++) {
				output[i] = cs.getObject(i + inArgTypes.length + 1);
			}
			return output;
		}
	}

	/**
	 * <p>Call a Stored Procedure/Function that will return a value.</p>
	 *
	 * <p>The function name should be package.function or schema.function (e.g. hca_utils_pack.get_tablespace_info).</p>
	 *
	 * <p>Stored Procedures/Functions with 1 or more OUT parameters should always use
	 * {@link #callFunction(Connection, String, Object[], int[], int[])}</p>
	 *
	 * <p><strong>Connection must be closed by the caller.</strong></p>
	 *
	 * @param conn An open Connection that will be used to call the function.
	 * @param name The name of the function.
	 * @param inArgs The IN arguments.
	 * @param inArgTypes The IN argument SQLTypes. Must be same size as inArgs.
	 * @param outArgTypes The RETURN value SQLType.
	 * @return The RETURN value.
	 * @throws SQLException
	 */
	public static Object callFunction(Connection conn, String name, Object[] inArgs, int[] inArgTypes, int returnType) throws SQLException {
		int totalArgs = inArgTypes.length;
		String args = IntStream.range(0, totalArgs).mapToObj(i -> "?").collect(Collectors.joining(","));
		try (CallableStatement cs = conn.prepareCall("{ ? = call "+ (isOracle() ? "phaedra." : "") + name + "(" + args + ") }")) {
			int arg = 1;
			// RETURN value.
			cs.registerOutParameter(arg++, returnType);
			// IN arguments.
			for (int i = 0; i < inArgs.length; i++) {
				cs.setObject(arg++, inArgs[i], inArgTypes[i]);
			}
			// Execute the function.
			cs.execute();

			// Gather the RETURN value.
			return cs.getObject(1);
		}
	}

	/*
	 * Locking of EntityManager (to avoid threading issues)
	 */

	public static Object queryWithLock(DatabaseQuery query, List<Object> args, EntityManager em) {
		lockEntityManager(em);
		try {
			Session session = getSession(em);
			Object result = session.executeQuery(query, args);
			return result;
		} finally {
			unlockEntityManager(em);
		}
	}

	public static List<?> queryWithLock(Query query, EntityManager em) {
		lockEntityManager(em);
		try {
			return query.getResultList();
		} finally {
			unlockEntityManager(em);
		}
	}

	public static void updateWithLock(Query query, EntityManager em) {
		lockEntityManager(em);
		try {
			query.executeUpdate();
		} finally {
			unlockEntityManager(em);
		}
	}

	public static void lockEntityManager(EntityManager em) {
		if (em instanceof LockingEntityManager) {
			((LockingEntityManager)em).getLock().lock();
		}
	}

	public static void unlockEntityManager(EntityManager em) {
		if (em instanceof LockingEntityManager) {
			((LockingEntityManager)em).getLock().unlock();
		}
	}

}