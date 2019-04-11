package eu.openanalytics.phaedra.model.subwell.data;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.sql.Array;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.persistence.PersistenceException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.postgresql.copy.CopyManager;
import org.postgresql.jdbc.PgConnection;

import eu.openanalytics.phaedra.base.db.JDBCUtils;
import eu.openanalytics.phaedra.base.environment.Screening;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.ProtocolService;
import eu.openanalytics.phaedra.model.protocol.vo.SubWellFeature;
import eu.openanalytics.phaedra.model.subwell.Activator;
import eu.openanalytics.phaedra.model.subwell.cache.SubWellDataCache;
import oracle.sql.ARRAY;
import oracle.sql.Datum;

//TODO Support string data
public class DBDataSource implements ISubWellDataSource {

	private static final String SCHEMA = "phaedra";
	private static final String DATA_TABLE = "hca_subwellfeature_value";

	@Override
	public void close() {
		// Nothing to do.
	}

	@Override
	public int getNrCells(Well well) {
		String sql = "select %s from %s.%s where well_id = %d %s";
		if (JDBCUtils.isPostgres()) {
			sql = String.format(sql, "array_length(num_val, 1)", SCHEMA, DATA_TABLE, well.getId(), "limit 1");
		} else if (JDBCUtils.isOracle()) {
			sql = String.format(sql, "phaedra.array_length(num_val, 1)", SCHEMA, DATA_TABLE, well.getId(), "and rownum = 1");
		}
		return select(sql, rs -> rs.next() ? rs.getInt(1) : 0, 0);
	}

	@Override
	public float[] getNumericData(Well well, SubWellFeature feature) {
		String sql = String.format("select num_val from %s.%s where well_id = %d and feature_id = %d", SCHEMA, DATA_TABLE, well.getId(), feature.getId());
		return select(sql, rs -> (rs.next()) ? getNumVal(rs) : null, 0);
	}

	@Override
	public String[] getStringData(Well well, SubWellFeature feature) {
		return null;
	}

	@Override
	public void preloadData(List<Well> wells, List<SubWellFeature> features, SubWellDataCache cache, IProgressMonitor monitor) {
		if (wells.isEmpty() || features.isEmpty()) return;
		
		// Mark the whole set as cached, so empty wells are not queried again later.
		for (Well well: wells) {
			for (SubWellFeature feature: features) {
				if (feature.isNumeric()) cache.putData(well, feature, (float[]) null);
				else cache.putData(well, feature, (String[]) null);
			}
		}
		
		String featureClause = "";
		if (features.size() < 200) {
			String featureIds = features.stream().map(f -> String.valueOf(f.getId())).collect(Collectors.joining(","));
			featureClause = String.format(" and feature_id in (%s)", featureIds);
		}

		String wellIds = wells.stream().map(w -> String.valueOf(w.getId())).collect(Collectors.joining(","));
		String sql = String.format("select * from %s.%s where well_id in (%s)%s order by well_id asc", SCHEMA, DATA_TABLE, wellIds, featureClause);
		select(sql, rs -> processResultSet(rs, wells, features, cache), 1000);
	}

	private Object processResultSet(ResultSet rs, List<Well> wells, List<SubWellFeature> features, SubWellDataCache cache) throws SQLException {
		Well currentWell = null;
		Map<SubWellFeature, Object> currentWellData = new HashMap<>();
		int currentCellCount = 0;
		
		while (rs.next()) {
			long wellId = rs.getLong("well_id");
			long featureId = rs.getLong("feature_id");
			
			if (currentWell == null || wellId != currentWell.getId()) {
				addToCache(currentWell, currentCellCount, currentWellData, cache);
				currentWell = wells.stream().filter(w -> w.getId() == wellId).findAny().orElse(null);
				currentWellData.clear();
				currentCellCount = 0;
			}
			
			SubWellFeature feature = ProtocolService.getInstance().getSubWellFeature(featureId);
			if (feature == null) continue;
			
			float[] numVal = getNumVal(rs);
			if (numVal == null) continue;
			
			currentCellCount = numVal.length;
			currentWellData.put(feature, numVal);
		}
		
		addToCache(currentWell, currentCellCount, currentWellData, cache);
		return null;
	}

	private void addToCache(Well well, int cellCount, Map<SubWellFeature, Object> data, SubWellDataCache cache) {
		if (well == null || data.isEmpty()) return;
		
		for (SubWellFeature feature: data.keySet()) {
			Object array = data.get(feature);
			if (array == null) continue;
			if (feature.isNumeric()) {
				cache.putData(well, feature, Arrays.copyOf((float[]) array, cellCount));
			} else {
				cache.putData(well, feature, Arrays.copyOf((String[]) array, cellCount));
			}
		}
	}

	@Override
	public void updateData(Map<SubWellFeature, Map<Well, Object>> data) {
		if (data.isEmpty()) return;
		data = data.entrySet().stream().filter(e -> e.getKey() != null).collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
		if (data.isEmpty()) return;
		
		String wellIds = data.values().stream().flatMap(m -> m.keySet().stream()).distinct().map(w -> String.valueOf(w.getId())).collect(Collectors.joining(","));
		String sql = String.format("select count(*) from %s.%s where well_id in (%s)", SCHEMA, DATA_TABLE, wellIds);
		int rowCount = select(sql, rs -> (rs.next()) ? rs.getInt(1) : 0, 0);
		
		if (rowCount == 0) {
			insertData(data);
		} else if (JDBCUtils.isPostgres()) {
			updateDataPostgres(data);
		} else if (JDBCUtils.isOracle()) {
			updateDataOracle(data);
		} else {
			//TODO
		}
	}

	private void updateDataPostgres(Map<SubWellFeature, Map<Well, Object>> data) {
		try (Connection conn = getConnection()) {
			boolean isAutoCommit = conn.getAutoCommit();
			conn.setAutoCommit(false);

			// Create a TEMP table
			try (Statement stmt = conn.createStatement()) {
				stmt.execute("create temp table if not exists tmp_subwellfeature_value (well_id bigint, feature_id bigint, num_val float[])");
				stmt.execute("delete from tmp_subwellfeature_value");
			}
			
			// Copy CSV data into the TEMP table
			CopyManager cm = conn.unwrap(PgConnection.class).getCopyAPI();
			try (InputStream input = getAsyncCSVDumper(data)) {
				cm.copyIn("copy tmp_subwellfeature_value from stdin (format csv)", input);
			}
			
			// Update the actual table using the TEMP table
			try (Statement stmt = conn.createStatement()) {
				String sql = String.format(
						" with upsert as ("
						+ "		update %s.%s sw"
						+ "		set num_val = tmp.num_val"
						+ "		from tmp_subwellfeature_value tmp"
						+ "		where sw.well_id = tmp.well_id and sw.feature_id = tmp.feature_id"
						+ "		returning sw.well_id as ex_well_id, sw.feature_id as ex_feature_id"
						+ "	)"
						+ "	insert into %s.%s (well_id, feature_id, num_val)"
						+ "		select tmp2.well_id, tmp2.feature_id, tmp2.num_val from tmp_subwellfeature_value tmp2"
						+ "		where not exists (select 1 from upsert u where u.ex_well_id = tmp2.well_id and u.ex_feature_id = tmp2.feature_id)",
						SCHEMA, DATA_TABLE, SCHEMA, DATA_TABLE);
				stmt.execute(sql);
			}
			
			conn.commit();
			conn.setAutoCommit(isAutoCommit);
		} catch (IOException | SQLException e) {
			throw new PersistenceException(e);
		}
	}
	
	private void updateDataOracle(Map<SubWellFeature, Map<Well, Object>> data) {
		try (Connection conn = getConnection()) {
			for (SubWellFeature feature: data.keySet()) {
				Map<Well, Object> featureData = data.get(feature);
				for (Well well: featureData.keySet()) {
					if (!feature.isNumeric() || featureData.get(well) == null) continue;
					
					StringBuilder values = new StringBuilder();
					values.append("phaedra.num_val_array(");
					float[] numVal = (float[]) featureData.get(well);
					for (int cellId = 0; cellId < numVal.length; cellId++) {
						values.append(String.valueOf(numVal[cellId]));
						if (cellId + 1 < numVal.length) values.append(",");
					}
					values.append(")");
					
					try (Statement stmt = conn.createStatement()) {
						String sql = String.format("merge into %s.%s using dual on (well_id = %d and feature_id = %d)"
								+ " when not matched then insert (well_id, feature_id, num_val) values (%d, %d, %s)"
								+ " when matched then update set num_val = %s",
								SCHEMA, DATA_TABLE, well.getId(), feature.getId(),
								well.getId(), feature.getId(), values.toString(),
								values.toString());
						stmt.execute(sql);
					}
				}
				conn.commit();
			}
		} catch (SQLException e) {
			throw new PersistenceException(e);
		}
	}
	
	private void insertData(Map<SubWellFeature, Map<Well, Object>> data) {
		List<Well> wells = data.values().stream().flatMap(m -> m.keySet().stream()).distinct().collect(Collectors.toList());
		if (data.isEmpty() || wells.isEmpty()) return;
		
		if (JDBCUtils.isPostgres()) {
			insertDataPostgres(data);
		} else if (JDBCUtils.isOracle()) {
			insertDataOracle(data);
		} else {
			//TODO
		}
	}
	
	private void insertDataPostgres(Map<SubWellFeature, Map<Well, Object>> data) {
		try (Connection conn = getConnection()) {
			String sql = String.format("copy %s.%s (well_id,feature_id,num_val) from stdin (format csv)", SCHEMA, DATA_TABLE);
			CopyManager cm = conn.unwrap(PgConnection.class).getCopyAPI();
			try (InputStream input = getAsyncCSVDumper(data)) {
				cm.copyIn(sql, input);
			}
			conn.commit();
		} catch (IOException | SQLException e) {
			throw new PersistenceException(e);
		}
	}
	
	private void insertDataOracle(Map<SubWellFeature, Map<Well, Object>> data) {
		try (Connection conn = getConnection()) {
			for (SubWellFeature feature: data.keySet()) {
				Map<Well, Object> featureData = data.get(feature);
				for (Well well: featureData.keySet()) {
					if (!feature.isNumeric() || featureData.get(well) == null) continue;
					
					StringBuilder values = new StringBuilder();
					values.append(well.getId() + ", " + feature.getId() + ", phaedra.num_val_array(");
					float[] numVal = (float[]) featureData.get(well);
					for (int cellId = 0; cellId < numVal.length; cellId++) {
						values.append(String.valueOf(numVal[cellId]));
						if (cellId + 1 < numVal.length) values.append(",");
					}
					values.append(")");
					
					try (Statement stmt = conn.createStatement()) {
						String sql = String.format("insert into %s.%s (well_id,feature_id,num_val) values (%s)", SCHEMA, DATA_TABLE, values.toString());
						stmt.execute(sql);
					}
				}
				conn.commit();
			}
		} catch (SQLException e) {
			throw new PersistenceException(e);
		}
	}
	
	private InputStream getAsyncCSVDumper(Map<SubWellFeature, Map<Well, Object>> data) throws IOException {
		PipedInputStream input = new PipedInputStream(1024*1024*50);
		PipedOutputStream output = new PipedOutputStream(input);
		new Thread(() -> { dumpToCSV(data, output); }, "Data Stream Copier").start();
		return input;
	}
	
	private void dumpToCSV(Map<SubWellFeature, Map<Well, Object>> data, OutputStream out) {
		try (BufferedWriter csvWriter = new BufferedWriter(new OutputStreamWriter(out))) {
			for (SubWellFeature feature: data.keySet()) {
				Map<Well, Object> featureData = data.get(feature);
				for (Well well: featureData.keySet()) {
					if (feature.isNumeric() && featureData.get(well) != null) {
						csvWriter.write(well.getId() + "," + feature.getId() + ",\"{");
						float[] numVal = (float[]) featureData.get(well);
						for (int cellId = 0; cellId < numVal.length; cellId++) {
							csvWriter.write(String.valueOf(numVal[cellId]));
							if (cellId + 1 < numVal.length) csvWriter.write(",");
						}
						csvWriter.write("}\"\n");
					}
				}
			}
			csvWriter.flush();
		} catch (Exception e) {
			EclipseLog.error(String.format("Failed to dump subwelldata to CSV"), e, Platform.getBundle(Activator.class.getPackage().getName()));
		}
	}

	private <T> T select(String sql, ResultProcessor<T> resultProcessor, int fetchSize) {
		long start = System.currentTimeMillis();
		try (Connection conn = getConnection()) {
			try (Statement stmt = conn.createStatement()) {
				if (fetchSize > 0) stmt.setFetchSize(fetchSize);
				try (ResultSet rs = stmt.executeQuery(sql)) {
					return resultProcessor.process(rs);
				}
			}
		} catch (SQLException e) {
			throw new RuntimeException("Failed to run query: " + sql, e);
		} finally {
			long duration = System.currentTimeMillis() - start;
			EclipseLog.debug(String.format("Query took %d ms: %s", duration, sql), DBDataSource.class);
		}
	}
	
	private float[] getNumVal(ResultSet rs) throws SQLException {
		Array numValArray = rs.getArray("num_val");
		if (numValArray == null) return null;
		
		if (JDBCUtils.isPostgres()) {
			Double[] doubleValues = (Double[]) numValArray.getArray();
			float[] floatValues = new float[doubleValues.length];
			for (int i=0; i<doubleValues.length; i++) floatValues[i] = doubleValues[i].floatValue();
			return floatValues;
		} else if (JDBCUtils.isOracle()) {
			Datum[] dValues = ((ARRAY) numValArray).getOracleArray();
			float[] floatValues = new float[dValues.length];
			for (int i=0; i<dValues.length; i++) floatValues[i] = dValues[i].floatValue();
			return floatValues;
		}
		
		return null;
	}
	
	private interface ResultProcessor<T> {
		public T process(ResultSet rs) throws SQLException;
	}
	
	private Connection getConnection() throws SQLException {
		return Screening.getEnvironment().getJDBCConnection();
	}
}
