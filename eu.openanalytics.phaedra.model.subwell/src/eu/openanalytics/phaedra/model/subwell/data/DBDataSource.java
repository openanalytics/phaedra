package eu.openanalytics.phaedra.model.subwell.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.persistence.PersistenceException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;

import eu.openanalytics.phaedra.base.environment.Screening;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.vo.SubWellFeature;
import eu.openanalytics.phaedra.model.subwell.Activator;
import eu.openanalytics.phaedra.model.subwell.cache.SubWellDataCache;

public class DBDataSource implements ISubWellDataSource {

	private static final String SCHEMA = "phaedra";
	private static final String DATA_TABLE = "hca_subwelldata";
	private static final String MAPPING_TABLE = "hca_sw_feature_map";
	
	private static final int MAX_FEATURES = 750;
	
	public DBDataSource() {
		try {
			checkTables();
		} catch (SQLException e) {
			throw new RuntimeException("Failed to set up database tables", e);
		}
	}

	@Override
	public void close() {
		// Nothing to do.
	}

	@Override
	public int getNrCells(Well well) {
		String sql = String.format("select count(*) from %s.%s where well_id = %d", SCHEMA, DATA_TABLE, well.getId());
		return select(sql, rs -> {
			rs.next();
			return rs.getInt(1);
		});
	}

	@Override
	public float[] getNumericData(Well well, SubWellFeature feature) {
		String sql = String.format("select f%d_num_val from %s.%s where well_id = %d order by cell_id asc", getFeatureIndex(feature), SCHEMA, DATA_TABLE, well.getId());
		return select(sql, rs -> {
			float[] values = new float[10000];
			int i=0;
			while (rs.next()) values[i++] = rs.getFloat(1);
			return Arrays.copyOf(values, i);
		});
	}

	@Override
	public String[] getStringData(Well well, SubWellFeature feature) {
		String sql = String.format("select f%d_str_val from %s.%s where well_id = %d order by cell_id asc", getFeatureIndex(feature), SCHEMA, DATA_TABLE, well.getId());
		return select(sql, rs -> {
			String[] values = new String[10000];
			int i=0;
			while (rs.next()) values[i++] = rs.getString(1);
			return Arrays.copyOf(values, i);
		});
	}

	@Override
	public void preloadData(List<Well> wells, List<SubWellFeature> features, SubWellDataCache cache, IProgressMonitor monitor) {
		String well_ids = wells.stream().map(w -> String.valueOf(w.getId())).collect(Collectors.joining(","));

		// First, retrieve the cell count of each well.
		Map<Well, Integer> cellCounts = new HashMap<>();
		String sql = String.format("select well_id, count(*) as cellCount from %s.%s where well_id in (%s) group by well_id", SCHEMA, DATA_TABLE, well_ids);
		select(sql, rs -> {
			while (rs.next()) {
				Well well = getWell(wells, rs.getLong("well_id"));
				if (well != null) cellCounts.put(well, rs.getInt("cellCount"));
			};
			return null;
		});

		String colNames = "*";
		if (features.size() < 200) colNames = getColNames(features);

		// Mark the whole set as cached, so empty wells are not queried again later.
		for (Well well: wells) {
			for (SubWellFeature feature: features) {
				if (feature.isNumeric()) cache.putData(well, feature, (float[]) null);
				else cache.putData(well, feature, (String[]) null);
			}
		}

		// The, retrieve the actual data for each well.
		sql = String.format("select %s from %s.%s where well_id in (%s) order by well_id asc", colNames, SCHEMA, DATA_TABLE, well_ids);
		select(sql, rs -> processResultSet(rs, cellCounts, wells, features, cache));
	}

	private Object processResultSet(ResultSet rs, Map<Well, Integer> cellCounts, List<Well> wells, List<SubWellFeature> features, SubWellDataCache cache) throws SQLException {
		Well currentWell = null;
		Map<SubWellFeature, Object> currentWellData = new HashMap<>();

		Map<SubWellFeature, Integer> featureIndices = new HashMap<>();
		for (SubWellFeature feature: features) {
			int index = getFeatureIndex(feature);
			featureIndices.put(feature, index);
		}

		while (rs.next()) {
			long well_id = rs.getLong("well_id");
			int cell_id = rs.getInt("cell_id");

			if (currentWell == null || well_id != currentWell.getId()) {
				addToCache(currentWell, currentWellData, cache);
				currentWell = getWell(wells, well_id);
				currentWellData.clear();
			}

			int cellCount = cellCounts.get(currentWell);
			for (SubWellFeature feature: features) {
				int index = featureIndices.get(feature);
				if (feature.isNumeric()) {
					float value = rs.getFloat(String.format("f%d_num_val", index));
					if (!currentWellData.containsKey(feature)) currentWellData.put(feature, new float[cellCount]); 
					float[] values = (float[]) currentWellData.get(feature);
					values[cell_id] = value;
				} else {
					String value = rs.getString(String.format("f%d_str_val", index));
					if (!currentWellData.containsKey(feature)) currentWellData.put(feature, new String[cellCount]); 
					String[] values = (String[]) currentWellData.get(feature);
					values[cell_id] = value;
				}
			}

		}

		addToCache(currentWell, currentWellData, cache);
		return null;
	}

	private void addToCache(Well well, Map<SubWellFeature, Object> data, SubWellDataCache cache) {
		if (well == null || data.isEmpty()) return;
		for (SubWellFeature feature: data.keySet()) {
			if (feature.isNumeric()) cache.putData(well, feature, (float[]) data.get(feature));
			else cache.putData(well, feature, (String[]) data.get(feature));
		}
	}

	@Override
	public void updateData(Map<SubWellFeature, Map<Well, Object>> data) {
		for (SubWellFeature feature: data.keySet()) {
			String colName = String.format(feature.isNumeric() ? "f%d_num_val" : "f%d_str_val", getFeatureIndex(feature));
			String queryString = String.format("update %s.%s set %s = ? where well_id = ? and cell_id = ?", SCHEMA, DATA_TABLE, colName);

			Map<Well, Object> featureData = data.get(feature);

			PreparedStatement ps = null;
			try (Connection conn = getConnection()) {
				ps = conn.prepareStatement(queryString);

				if (feature.isNumeric()) {
					for (Well well: featureData.keySet()) {
						float[] numVal = (float[]) featureData.get(well);
						for (int cell_id = 0; cell_id < numVal.length; cell_id++) {
							ps.setFloat(1, numVal[cell_id]);
							ps.setLong(2, well.getId());
							ps.setLong(3, cell_id);
							ps.addBatch();
						}
					}
				} else {
					for (Well well: featureData.keySet()) {
						String[] strVal = (String[]) featureData.get(well);
						for (int cell_id = 0; cell_id < strVal.length; cell_id++) {
							ps.setString(1, strVal[cell_id]);
							ps.setLong(2, well.getId());
							ps.setLong(3, cell_id);
							ps.addBatch();
						}
					}
				}

				ps.executeBatch();
				conn.commit();
			} catch (SQLException e) {
				throw new PersistenceException(e);
			} finally {
				if (ps != null) try { ps.close(); } catch (SQLException e) {}
			}
		}
	}

	private Well getWell(List<Well> wells, long well_id) {
		return wells.stream().filter(w -> w.getId() == well_id).findAny().orElse(null);
	}

	private int getFeatureIndex(SubWellFeature feature) {
		// Get current mappings.
		String sql = String.format("select feature_id, sequence_nr from %s.%s where protocolclass_id = %d", SCHEMA, MAPPING_TABLE, feature.getProtocolClass().getId());
		long[] mapping = select(sql, rs -> {
			long[] m = new long[MAX_FEATURES];
			while (rs.next()) m[rs.getInt(2)] = rs.getLong(1);
			return m;
		});
		
		// Find existing mapping for this feature.
		for (int i = 0; i < mapping.length; i++) {
			if (mapping[i] == feature.getId()) return i;
		}
		
		// Create new mapping for this feature.
		for (int i = 0; i < mapping.length; i++) {
			//TODO Recycle old mappings. This will start failing after MAX_FEATURES.
			if (mapping[i] == 0) {
				createFeatureMapping(feature, i);
				return i;
			}
		}
		
		throw new RuntimeException("Failed to obtain a feature index mapping for " + feature);
	}
	
	private void createFeatureMapping(SubWellFeature feature, int index) {
		String queryString = String.format("insert into %s.%s (protocolclass_id,feature_id,sequence_nr) values (?,?,?)",
				SCHEMA, DATA_TABLE, feature.getProtocolClass().getId(), feature.getId(), index);
		try (Connection conn = getConnection()) {
			try (Statement stmt = conn.createStatement()) {
				stmt.executeQuery(queryString);
			}
		} catch (SQLException e) {
			throw new PersistenceException(e);
		}
	}

	private <T> T select(String sql, ResultProcessor<T> resultProcessor) {
		long start = System.currentTimeMillis();
		try (Connection conn = getConnection()) {
			try (Statement stmt = conn.createStatement()) {
				try (ResultSet rs = stmt.executeQuery(sql)) {
					return resultProcessor.process(rs);
				}
			}
		} catch (SQLException e) {
			throw new RuntimeException("Failed to run query: " + sql, e);
		} finally {
			long duration = System.currentTimeMillis() - start;
			EclipseLog.info(String.format("Query took %d ms: %s", duration, sql), Platform.getBundle(Activator.class.getPackage().getName()));
		}
	}
	
	private void executeStatement(String sql, Connection conn) throws SQLException {
		try (Statement stmt = conn.createStatement()) {
			stmt.execute(sql.toString());
		}
	}

	private String getColNames(Collection<SubWellFeature> features) {
		return  "well_id,cell_id," + features.stream()
			.map(f -> String.format(f.isNumeric() ? "f%d_num_val" : "f%d_str_val", getFeatureIndex(f)))
			.collect(Collectors.joining(","));	
	}

	private interface ResultProcessor<T> {
		public T process(ResultSet rs) throws SQLException;
	}
	
	private Connection getConnection() throws SQLException {
		return Screening.getEnvironment().getJDBCConnection();
	}

	private void checkTables() throws SQLException {
		try (Connection conn = getConnection()) {
			try {
				executeStatement(String.format("select * from %s.%s limit 1", SCHEMA, DATA_TABLE), conn);
			} catch (SQLException e) {
				conn.rollback();

				//TODO this requires table creation privileges
				StringBuilder sql = new StringBuilder();
				sql.append(String.format("create table %s.%s (", SCHEMA, DATA_TABLE));
				sql.append("well_id bigint, cell_id bigint, ");
				for (int i = 0; i < MAX_FEATURES; i++) {
					sql.append(String.format("f%d_num_val float, f%d_str_val varchar(100),", i, i));
				}
				sql.deleteCharAt(sql.length() - 1);
				sql.append(") tablespace phaedra_d");
				executeStatement(sql.toString(), conn);
				
				sql = new StringBuilder();
				sql.append(String.format("create table %s.%s (", SCHEMA, MAPPING_TABLE));
				sql.append("protocolclass_id bigint, feature_id bigint, sequence_nr integer) tablespace phaedra_d");
				executeStatement(sql.toString(), conn);
				conn.commit();
				
				executeStatement(String.format("alter table %s.%s add constraint %s_pk primary key (well_id, cell_id) using index tablespace phaedra_i", SCHEMA, DATA_TABLE, DATA_TABLE), conn);
				executeStatement(String.format("alter table %s.%s add constraint %s_fk_well foreign key (well_id) references %s.hca_plate_well(well_id) on delete cascade", SCHEMA, DATA_TABLE, DATA_TABLE, SCHEMA), conn);
				
				executeStatement(String.format("grant INSERT, UPDATE, DELETE on %s.%s to phaedra_role_crud", SCHEMA, DATA_TABLE), conn);
				executeStatement(String.format("grant SELECT on %s.%s to phaedra_role_read", SCHEMA, DATA_TABLE), conn);
				executeStatement(String.format("grant INSERT, UPDATE, DELETE on %s.%s to phaedra_role_crud", SCHEMA, MAPPING_TABLE), conn);
				executeStatement(String.format("grant SELECT on %s.%s to phaedra_role_read", SCHEMA, MAPPING_TABLE), conn);
				conn.commit();
			}
		}
	}
}
