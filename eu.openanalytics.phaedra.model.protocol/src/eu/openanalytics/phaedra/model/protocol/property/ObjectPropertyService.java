package eu.openanalytics.phaedra.model.protocol.property;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;

import eu.openanalytics.phaedra.base.db.JDBCUtils;
import eu.openanalytics.phaedra.base.db.jpa.BaseJPAService;
import eu.openanalytics.phaedra.base.environment.Screening;
import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.base.security.model.Roles;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;

public class ObjectPropertyService extends BaseJPAService {

	private static ObjectPropertyService instance = new ObjectPropertyService();
	
	private static final String DB_SCHEMA = "phaedra";
	private static final String DB_TABLE = "hca_object_property";
	
	private ObjectPropertyService() {
		// Hidden constructor
	}
	
	public static ObjectPropertyService getInstance() {
		return instance;
	}
	
	@Override
	protected EntityManager getEntityManager() {
		return Screening.getEnvironment().getEntityManager();
	}
	
	/**
	 * Public API
	 * **********
	 */

	public String[] getPropertyNames(String objectType, long objectId) {
		checkPermission(objectType, objectId, true);
		String sql = String.format("select property_name from %s.%s where object_class = '%s' and object_id = %d",
				DB_SCHEMA, DB_TABLE, objectType, objectId);
		List<String> propNames = new ArrayList<>();
		select(sql, rs -> {
			while (rs.next()) {
				propNames.add(rs.getString(1));
			}
		}, 0);
		return propNames.toArray(new String[propNames.size()]);
	}
	
	public Map<String, Object> getProperties(String objectType, long objectId) {
		checkPermission(objectType, objectId, true);
		String sql = String.format("select property_name, numeric_value, string_value, binary_value from %s.%s where object_class = '%s' and object_id = %d",
				DB_SCHEMA, DB_TABLE, objectType, objectId);
		Map<String, Object> properties = new HashMap<>();
		select(sql, rs -> {
			while (rs.next()) {
				Object value = rs.getBigDecimal(2);
				if (value == null) value = rs.getString(3);
				if (value == null) value = rs.getBytes(4);
				properties.put(rs.getString(1), value);
			}
		}, 0);
		return properties;
	}
	
	public float getNumericValue(String objectType, long objectId, String propertyName) {
		checkPermission(objectType, objectId, true);
		BigDecimal value = getValue(objectType, objectId, propertyName, "numeric_value", rs -> rs.getBigDecimal(1));
		return value == null ? Float.NaN : value.floatValue();
	}
	
	public String getStringValue(String objectType, long objectId, String propertyName) {
		checkPermission(objectType, objectId, true);
		return getValue(objectType, objectId, propertyName, "string_value", rs -> rs.getString(1));
	}
	
	public byte[] getBinaryValue(String objectType, long objectId, String propertyName) {
		checkPermission(objectType, objectId, true);
		return getValue(objectType, objectId, propertyName, "binary_value", rs -> rs.getBytes(1));
	}
	
	public float[] getNumericValues(String objectType, long[] objectIds, String propertyName) {
		checkPermission(objectType, objectIds, true);
		return (float[]) getValues(objectType, objectIds, propertyName, "numeric_value");
	}
	
	public String[] getStringValues(String objectType, long[] objectIds, String propertyName) {
		checkPermission(objectType, objectIds, true);
		return (String[]) getValues(objectType, objectIds, propertyName, "string_value");
	}
	
	public byte[][] getBinaryValues(String objectType, long[] objectIds, String propertyName) {
		checkPermission(objectType, objectIds, true);
		return (byte[][]) getValues(objectType, objectIds, propertyName, "binary_value");
	}
	
	public void setValue(String objectType, long objectId, String propertyName, float numericValue) {
		checkPermission(objectType, objectId, true);
		setValueObject(objectType, objectId, propertyName, numericValue);
	}
	
	public void setValue(String objectType, long objectId, String propertyName, String stringValue) {
		checkPermission(objectType, objectId, true);
		setValueObject(objectType, objectId, propertyName, stringValue);
	}
	
	public void setValue(String objectType, long objectId, String propertyName, byte[] binaryValue) {
		checkPermission(objectType, objectId, true);
		setValueObject(objectType, objectId, propertyName, binaryValue);
	}
	
	public void setValues(String objectType, long[] objectIds, String propertyName, float[] numericValues) {
		checkPermission(objectType, objectIds, true);
		setValueObjects(objectType, objectIds, propertyName, numericValues);
	}
	
	public void setValues(String objectType, long[] objectIds, String propertyName, String[] stringValues) {
		checkPermission(objectType, objectIds, true);
		setValueObjects(objectType, objectIds, propertyName, stringValues);
	}
	
	public void setValues(String objectType, long[] objectIds, String propertyName, byte[][] binaryValues) {
		checkPermission(objectType, objectIds, true);
		setValueObjects(objectType, objectIds, propertyName, binaryValues);
	}
	
	public void deleteValue(String objectType, long objectId, String propertyName) {
		checkPermission(objectType, objectId, true);
		String sql = String.format("delete from %s.%s where object_class = '%s' and object_id = %d and property_name =  '%s'",
				DB_SCHEMA, DB_TABLE, objectType, objectId, propertyName);
		execute(sql, null, false);
	}
	
	public void deleteValues(String objectType, long objectId) {
		checkPermission(objectType, objectId, true);
		String sql = String.format("delete from %s.%s where object_class = '%s' and object_id = %d",
				DB_SCHEMA, DB_TABLE, objectType, objectId);
		execute(sql, null, false);
	}
	
	public void deleteValues(String objectType, long[] objectIds, String propertyName) {
		checkPermission(objectType, objectIds, true);
		String sql = String.format("delete from %s.%s where object_class = '%s' and object_id in (%s) and property_name =  '%s'",
				DB_SCHEMA, DB_TABLE, objectType, getIdList(objectIds), propertyName);
		execute(sql, null, false);
	}
	
	public void deleteValues(String objectType, long[] objectIds) {
		checkPermission(objectType, objectIds, true);
		String sql = String.format("delete from %s.%s where object_class = '%s' and object_id in (%s)",
				DB_SCHEMA, DB_TABLE, objectType, getIdList(objectIds));
		execute(sql, null, false);
	}
	
	public String getObjectType(Object object) {
		return object == null ? null : object.getClass().getName();
	}
	
	/**
	 * Non-public
	 * **********
	 */
	
	private boolean checkPermission(String objectType, long objectId, boolean throwException) {
		Object object = getEntity(objectType, objectId);
		if (object == null) {
			if (throwException) throw new RuntimeException(String.format("Object not found: %s %d", objectType, objectId));
			return false;
		}
		if (throwException) return SecurityService.getInstance().checkWithException(Roles.USER, object);
		else return SecurityService.getInstance().check(Roles.USER, object);
	}
	
	private boolean checkPermission(String objectType, long[] objectIds, boolean throwException) {
		boolean allChecked = true;
		for (long objectId: objectIds) {
			allChecked &= checkPermission(objectType, objectId, throwException);
		}
		return allChecked;
	}
	
	private <T> T getValue(String objectType, long objectId, String propertyName, String colName, ValueMapper<T> mapper) {
		String sql = String.format("select %s from %s.%s where object_class = '%s' and object_id = %d and property_name =  '%s'",
				colName, DB_SCHEMA, DB_TABLE, objectType, objectId, propertyName);
		Pair<String, T> value = new MutablePair<>(propertyName, null);
		select(sql, rs -> {
			if (rs.next()) value.setValue(mapper.map(rs));
		}, 0);
		return value.getValue();
	}
	
	private Object getValues(String objectType, long[] objectIds, String propertyName, String colName) {
		String sql = String.format("select %s from %s.%s where object_class = '%s' and object_id in (%s) and property_name =  '%s' order by object_id asc",
				colName, DB_SCHEMA, DB_TABLE, objectType, getIdList(objectIds), propertyName);
		
		Object retVal = null;
		switch (colName) {
		case "numeric_value":
			float[] numericValues = new float[objectIds.length];
			select(sql, rs -> {
				int i = 0;
				while (rs.next()) numericValues[i++] = rs.getFloat(1);
			}, 0);
			retVal = numericValues;
			break;
		case "string_value":
			String[] stringValues = new String[objectIds.length];
			select(sql, rs -> {
				int i = 0;
				while (rs.next()) stringValues[i++] = rs.getString(1);
			}, 0);
			retVal = stringValues;
			break;
		case "binary_value":
			byte[][] binaryValues = new byte[objectIds.length][];
			select(sql, rs -> {
				int i = 0;
				while (rs.next()) binaryValues[i++] = rs.getBytes(1);
			}, 0);
			retVal = binaryValues;
			break;
		}
		
		// Put the results in the same order as the provided objectIds
		long[] sortedIds = Arrays.copyOf(objectIds, objectIds.length);
		Arrays.sort(sortedIds);
		if (retVal instanceof float[]) {
			float[] newRetVal = new float[objectIds.length];
			for (int i = 0; i < newRetVal.length; i++) {
				int index = findIndex(sortedIds, objectIds[i]);
				newRetVal[i] = ((float[]) retVal)[index];
			}
			retVal = newRetVal;
		} else if (retVal instanceof String[]) {
			String[] newRetVal = new String[objectIds.length];
			for (int i = 0; i < newRetVal.length; i++) {
				int index = findIndex(sortedIds, objectIds[i]);
				newRetVal[i] = ((String[]) retVal)[index];
			}
			retVal = newRetVal;
		} else if (retVal instanceof byte[][]) {
			byte[][] newRetVal = new byte[objectIds.length][];
			for (int i = 0; i < newRetVal.length; i++) {
				int index = findIndex(sortedIds, objectIds[i]);
				newRetVal[i] = ((byte[][]) retVal)[index];
			}
			retVal = newRetVal;
		}
		
		return retVal;
	}
	
	private void setValueObject(String objectType, long objectId, String propertyName, Object value) {
		if (JDBCUtils.isEmbedded()) deleteValue(objectType, objectId, propertyName);
		String sql = String.format(
				"insert into %s.%s (object_class, object_id, property_name, numeric_value, string_value, binary_value) values ('%s', %d, '%s', ?, ?, ?)",
				DB_SCHEMA, DB_TABLE, objectType, objectId, propertyName);
		sql = appendUpsert(sql);
		execute(sql, stmt -> {
			if (value instanceof Number) stmt.setFloat(1, ((Number) value).floatValue());
			else stmt.setNull(1, Types.FLOAT);
			stmt.setString(2, (value instanceof String) ? ((String) value) : null);
			stmt.setBytes(3, (value instanceof byte[]) ? ((byte[]) value) : null);
		}, false);
	}
	
	private void setValueObjects(String objectType, long[] objectIds, String propertyName, Object values) {
		if (JDBCUtils.isEmbedded()) deleteValues(objectType, objectIds, propertyName);
		String sql = String.format(
				"insert into %s.%s (object_class, object_id, property_name, numeric_value, string_value, binary_value) values ('%s', ?, '%s', ?, ?, ?)",
				DB_SCHEMA, DB_TABLE, objectType, propertyName);
		sql = appendUpsert(sql);
		execute(sql, stmt -> {
			for (int i = 0; i < objectIds.length; i++) {
				stmt.setLong(1, objectIds[i]);
				if (values instanceof float[]) stmt.setFloat(2, ((float[]) values)[i]);
				else stmt.setNull(2, Types.FLOAT);
				stmt.setString(3, (values instanceof String[]) ? ((String[]) values)[i] : null);
				stmt.setBytes(4, (values instanceof byte[][]) ? ((byte[][]) values)[i] : null);
				stmt.addBatch();
			}
		}, true);
	}
	
	private String appendUpsert(String sql) {
		if (JDBCUtils.isPostgres()) {
			sql += " on conflict do update"
				+ " set numeric_value = excluded.numeric_value, string_value = excluded.string_value, binary_value = excluded.binary_value";
		} else if (JDBCUtils.isOracle()) {
			sql += " on duplicate key update"
				+ " numeric_value = values(numeric_value), string_value = values(string_value), binary_value = values(binary_value)";
		} else {
			// Embedded/H2: upsert not supported. Instead, do a delete + insert
		}
		return sql;
	}
	
	private void select(String sql, ResultProcessor rsProcessor, int fetchSize) {
		long start = System.currentTimeMillis();
		try (Connection conn = getConnection()) {
			try (Statement stmt = conn.createStatement()) {
				if (fetchSize > 0) stmt.setFetchSize(fetchSize);
				try (ResultSet rs = stmt.executeQuery(sql)) {
					rsProcessor.process(rs);
				}
			}
		} catch (SQLException e) {
			throw new RuntimeException("Failed to run query: " + sql, e);
		} finally {
			long duration = System.currentTimeMillis() - start;
			EclipseLog.debug(String.format("Query took %d ms: %s", duration, sql), ObjectPropertyService.class);
		}
	}
	
	private void execute(String sql, PreparedStatementSetter setter, boolean batch) {
		long start = System.currentTimeMillis();
		try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
			if (setter != null) setter.prepare(stmt);
			if (batch) stmt.executeBatch();
			else stmt.execute();
			conn.commit();
		} catch (SQLException e) {
			throw new RuntimeException("Failed to run statement: " + sql, e);
		} finally {
			long duration = System.currentTimeMillis() - start;
			EclipseLog.debug(String.format("Query took %d ms: %s", duration, sql), ObjectPropertyService.class);
		}
	}
	
	private String getIdList(long[] ids) {
		return Arrays.stream(ids).mapToObj(id -> String.valueOf(id)).collect(Collectors.joining(","));
	}
	
	private int findIndex(long[] ids, long id) {
		for (int i = 0; i < ids.length; i++) {
			if (ids[i] == id) return i;
		}
		return -1;
	}
	
	private interface ResultProcessor {
		public void process(ResultSet rs) throws SQLException;
	}
	
	private interface ValueMapper<T> {
		public T map(ResultSet rs) throws SQLException;
	}
	
	private interface PreparedStatementSetter {
		public void prepare(PreparedStatement stmt) throws SQLException;
	}
	
	private Connection getConnection() throws SQLException {
		return Screening.getEnvironment().getJDBCConnection();
	}
}
