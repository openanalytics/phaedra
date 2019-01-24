package eu.openanalytics.phaedra.model.subwell.data;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import javax.persistence.PersistenceException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.postgresql.copy.CopyManager;
import org.postgresql.jdbc.PgConnection;

import eu.openanalytics.phaedra.base.db.JDBCUtils;
import eu.openanalytics.phaedra.base.environment.Screening;
import eu.openanalytics.phaedra.base.event.ModelEventService;
import eu.openanalytics.phaedra.base.event.ModelEventType;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.model.protocol.vo.SubWellFeature;
import eu.openanalytics.phaedra.model.subwell.Activator;
import eu.openanalytics.phaedra.model.subwell.cache.SubWellDataCache;

public class DBDataSource implements ISubWellDataSource {

	private static final String SCHEMA = "phaedra";
	private static final String DATA_TABLE = "hca_subwelldata";
	private static final String MAPPING_TABLE = "hca_subwelldata_feature";
	
	private static final int MAX_FEATURES = JDBCUtils.isOracle() ? 998 : 1500;
	private static final int MAX_ROWS_PER_WELL = 30000;

	private String dataInsertTable = DATA_TABLE;
	
	public DBDataSource() {
		ModelEventService.getInstance().addEventListener(event -> {
			if (event.type == ModelEventType.ObjectRemoved && event.source instanceof Plate) {
				deletePlateData((Plate) event.source);
			}
		});
		
		String diTable = Screening.getEnvironment().getConfig().getValue(Screening.getEnvironment().getName(), "db", "subwelldata.inserttable");
		if (diTable != null && !diTable.isEmpty()) dataInsertTable = diTable;
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
		int featureIndex = getFeatureMapping(feature.getProtocolClass()).get(feature);
		String sql = String.format("select f%d_num_val from %s.%s where well_id = %d order by cell_id asc", featureIndex, SCHEMA, DATA_TABLE, well.getId());
		return select(sql, rs -> {
			float[] values = new float[MAX_ROWS_PER_WELL];
			int i=0;
			while (rs.next()) values[i++] = rs.getFloat(1);
			return Arrays.copyOf(values, i);
		});
	}

	@Override
	public String[] getStringData(Well well, SubWellFeature feature) {
		int featureIndex = getFeatureMapping(feature.getProtocolClass()).get(feature);
		String sql = String.format("select f%d_str_val from %s.%s where well_id = %d order by cell_id asc", featureIndex, SCHEMA, DATA_TABLE, well.getId());
		return select(sql, rs -> {
			String[] values = new String[MAX_ROWS_PER_WELL];
			int i=0;
			while (rs.next()) values[i++] = rs.getString(1);
			return Arrays.copyOf(values, i);
		});
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
		
		Map<SubWellFeature, Integer> featureMappingAll = getFeatureMapping(ProtocolUtils.getProtocolClass(wells.get(0)));
		Map<SubWellFeature, Integer> featureMapping = new HashMap<>();
		for (SubWellFeature f: features) {
			featureMapping.put(f, featureMappingAll.get(f));
		}
		
		String colNames = "*";
		if (features.size() < 200) colNames = getColNames(featureMapping);

		String well_ids = wells.stream().map(w -> String.valueOf(w.getId())).collect(Collectors.joining(","));
		String sql = String.format("select %s from %s.%s where well_id in (%s) order by well_id asc", colNames, SCHEMA, DATA_TABLE, well_ids);
		select(sql, rs -> processResultSet(rs, wells, featureMapping, cache));
	}

	private Object processResultSet(ResultSet rs, List<Well> wells, Map<SubWellFeature, Integer> features, SubWellDataCache cache) throws SQLException {
		Well currentWell = null;
		Map<SubWellFeature, Object> currentWellData = new HashMap<>();

		BiFunction<SubWellFeature, Integer, Object> dataArraySupplier = (feature, minSize) -> {
			int newSize = 1000;
			while (newSize < minSize) newSize += 1000;
			
			Object array = currentWellData.get(feature);
			
			if (array == null) {
				// Create a new array with sufficient size
				if (feature.isNumeric()) array = new float[newSize];
				else array = new String[newSize];
				currentWellData.put(feature, array);
			} else {
				// Ensure that the array has sufficient size
				if (feature.isNumeric()) {
					float[] numArray = (float[]) array;
					if (numArray.length < minSize) {
						array = Arrays.copyOf(numArray, newSize);
						currentWellData.put(feature, array);
					}
				} else {
					String[] strArray = (String[]) array;
					if (strArray.length < minSize) {
						array = Arrays.copyOf(strArray, newSize);
						currentWellData.put(feature, array);
					}
				}
			}
			
			return array;
		};
		
		int cellCount = 0;
		
		while (rs.next()) {
			long wellId = rs.getLong("well_id");
			int cellId = rs.getInt("cell_id");
			
			if (currentWell == null || wellId != currentWell.getId()) {
				addToCache(currentWell, cellCount, currentWellData, cache);
				currentWell = getWell(wells, wellId);
				currentWellData.clear();
				cellCount = 0;
			}

			cellCount = Math.max(cellCount, cellId + 1);
			
			for (SubWellFeature feature: features.keySet()) {
				int index = features.get(feature);
				if (feature.isNumeric()) {
					float value = rs.getFloat(String.format("f%d_num_val", index));
					float[] values = (float[]) dataArraySupplier.apply(feature, cellCount);
					values[cellId] = value;
				} else {
					String value = rs.getString(String.format("f%d_str_val", index));
					String[] values = (String[]) dataArraySupplier.apply(feature, cellCount);
					values[cellId] = value;
				}
			}
		}

		addToCache(currentWell, cellCount, currentWellData, cache);
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
		
		SubWellFeature sample = data.keySet().iterator().next();
		if (sample == null) return;
		Map<SubWellFeature, Integer> featureMapping = getFeatureMapping(sample.getProtocolClass());
		
		String wellIds = data.values().stream().flatMap(m -> m.keySet().stream()).distinct().map(w -> String.valueOf(w.getId())).collect(Collectors.joining(","));
		String sql = String.format("select count(*) from %s.%s where well_id in (%s)", SCHEMA, DATA_TABLE, wellIds);
		int rowCount = select(sql, rs -> {
			rs.next();
			return rs.getInt(1);
		});
		if (rowCount == 0) {
			insertData(data, featureMapping);
			return;
		}
		if (JDBCUtils.isPostgres()) {
			updateDataPostgres(data, featureMapping);
			return;
		}
		
		for (SubWellFeature feature: data.keySet()) {
			String colName = String.format(feature.isNumeric() ? "f%d_num_val" : "f%d_str_val", featureMapping.get(feature));
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

	private void updateDataPostgres(Map<SubWellFeature, Map<Well, Object>> data, Map<SubWellFeature, Integer> featureMapping) {
		try (Connection conn = getConnection()) {
			boolean isAutoCommit = conn.getAutoCommit();
			conn.setAutoCommit(false);
			
			for (SubWellFeature feature: data.keySet()) {
				Map<Well, Object> featureData = data.get(feature);
				
				// Format the data as CSV
				ByteArrayOutputStream csv = new ByteArrayOutputStream();
				BufferedWriter csvWriter = new BufferedWriter(new OutputStreamWriter(csv));
				for (Well well: featureData.keySet()) {
					if (feature.isNumeric()) {
						float[] numVal = (float[]) featureData.get(well);
						for (int cell_id = 0; cell_id < numVal.length; cell_id++) {
							csvWriter.write(well.getId() + "," + cell_id + "," + numVal[cell_id] + "\n");
						}
					}
				}
				csvWriter.flush();
				byte[] csvBytes = csv.toByteArray();
				
				// Create a TEMP table
				try (Statement stmt = conn.createStatement()) {
					stmt.execute("create temp table if not exists tmp_subwelldata (well_id bigint, cell_id bigint, num_val float)");
					stmt.execute("delete from tmp_subwelldata");
				}
				
				// Put the new data into the TEMP table
				CopyManager cm = conn.unwrap(PgConnection.class).getCopyAPI();
				cm.copyIn("copy tmp_subwelldata from stdin (format csv)", new ByteArrayInputStream(csvBytes));
				
				// Update the actual table using the TEMP table
				try (Statement stmt = conn.createStatement()) {
					String colName = String.format("f%d_num_val", featureMapping.get(feature));
					String sql = String.format("update %s.%s sw set %s = tmp.num_val from tmp_subwelldata tmp"
							+ " where sw.well_id = tmp.well_id and sw.cell_id = tmp.cell_id", SCHEMA, DATA_TABLE, colName);
					stmt.execute(sql);
				}
			}
			
			conn.commit();
			conn.setAutoCommit(isAutoCommit);
		} catch (IOException | SQLException e) {
			throw new PersistenceException(e);
		}
	}
	
	private void insertData(Map<SubWellFeature, Map<Well, Object>> data, Map<SubWellFeature, Integer> featureMapping) {
		List<Well> wells = data.values().stream().flatMap(m -> m.keySet().stream()).distinct().collect(Collectors.toList());
		if (data.isEmpty() || wells.isEmpty()) return;
		
		if (JDBCUtils.isPostgres()) {
			insertDataPostgres(data, featureMapping);
			return;
		}

		try (Connection conn = getConnection()) {
			for (Well well: wells) {
				Map<SubWellFeature, Object> dataForWell = new HashMap<>();
				data.keySet().stream().forEach(f -> dataForWell.put(f, data.get(f).get(well)));
				if (dataForWell.isEmpty()) continue;
				
				List<SubWellFeature> orderedFeatures = new ArrayList<>(dataForWell.keySet());
				String colNames = orderedFeatures.stream()
						.map(f -> String.format(f.isNumeric() ? "f%d_num_val" : "f%d_str_val", featureMapping.get(f)))
						.collect(Collectors.joining(","));
				String args = dataForWell.keySet().stream().map(f -> "?").collect(Collectors.joining(","));
				String queryString = String.format("insert into %s.%s(well_id,cell_id," + colNames + ") values (?,?," + args + ")", SCHEMA, dataInsertTable);
				
				// Determine nr of cells (rows) for this well
				int cellCount = 0;
				for (SubWellFeature f: orderedFeatures) {
					Object o = dataForWell.get(f);
					if (o instanceof float[]) { cellCount = ((float[]) o).length; break; }
					else if (o instanceof String[]) { cellCount = ((String[]) o).length; break; }
				}
				if (cellCount == 0) continue;
				
				try (PreparedStatement ps = conn.prepareStatement(queryString)) {
					for (int cellId = 0; cellId < cellCount; cellId++) {
						ps.setLong(1, well.getId());
						ps.setLong(2, cellId);
						for (int i = 0; i < orderedFeatures.size(); i++) {
							SubWellFeature f = orderedFeatures.get(i);
							if (f.isNumeric()) {
								float[] d = (float[]) dataForWell.get(f);
								float value = (d == null || d.length <= cellId) ? Float.NaN : d[cellId];
								ps.setFloat(3+i, value);
							}
							else {
								String[] d = (String[]) dataForWell.get(f);
								String value = (d == null || d.length <= cellId) ? null : d[cellId];
								ps.setString(3+i, value);
							}
						}
						ps.addBatch();
					}
					ps.executeBatch();
				}
			}
			conn.commit();
		} catch (SQLException e) {
			throw new PersistenceException(e);
		}
	}
	
	private void insertDataPostgres(Map<SubWellFeature, Map<Well, Object>> data, Map<SubWellFeature, Integer> featureMapping) {
		List<Well> wells = data.values().stream().flatMap(m -> m.keySet().stream()).distinct().collect(Collectors.toList());
		if (data.isEmpty() || wells.isEmpty()) return;
		
		try (Connection conn = getConnection()) {
			List<SubWellFeature> orderedFeatures = new ArrayList<>(data.keySet());
			String colNames = orderedFeatures.stream()
					.map(f -> String.format(f.isNumeric() ? "f%d_num_val" : "f%d_str_val", featureMapping.get(f)))
					.collect(Collectors.joining(","));
			String sql = String.format("copy %s.%s (well_id,cell_id,%s) from stdin (format csv)", SCHEMA, DATA_TABLE, colNames);
			
			CopyManager cm = conn.unwrap(PgConnection.class).getCopyAPI();
			try (InputStream input = createDataStreamCopier(data, orderedFeatures)) {
				cm.copyIn(sql, input);
			}
			
			conn.commit();
		} catch (IOException | SQLException e) {
			throw new PersistenceException(e);
		}
	}
	
	private InputStream createDataStreamCopier(Map<SubWellFeature, Map<Well, Object>> data, List<SubWellFeature> orderedFeatures) throws IOException {
		List<Well> wells = data.values().stream().flatMap(m -> m.keySet().stream()).distinct().collect(Collectors.toList());
		
		PipedInputStream input = new PipedInputStream(1024*1024*50);
		PipedOutputStream output = new PipedOutputStream(input);

		Runnable dataCopier = () -> {
			try (BufferedWriter csvWriter = new BufferedWriter(new OutputStreamWriter(output))) {
				for (Well well: wells) {
					int cellCount = data.keySet().stream().mapToInt(f -> {
						Object values = data.get(f).get(well);
						if (values instanceof float[]) return ((float[]) values).length;
						else if (values instanceof String[]) return ((String[]) values).length;
						else return 0;
					}).max().orElse(0);
					
					for (int cellId = 0; cellId < cellCount; cellId++) {
						String[] line = new String[orderedFeatures.size() + 2];
						line[0] = String.valueOf(well.getId());
						line[1] = String.valueOf(cellId);
						
						for (int fIndex = 0; fIndex < orderedFeatures.size(); fIndex++) {
							SubWellFeature f = orderedFeatures.get(fIndex);
							if (f.isNumeric()) {
								float[] numVal = (float[]) data.get(f).get(well);
								if (numVal == null || cellId >= numVal.length) line[fIndex + 2] = "";
								else line[fIndex + 2] = String.valueOf(numVal[cellId]);						
							} else {
								String[] strVal = (String[]) data.get(f).get(well);
								if (strVal == null || cellId >= strVal.length) line[fIndex + 2] = "";
								else line[fIndex + 2] = strVal[cellId];
							}
						}
						
						String lineStr = Arrays.stream(line).collect(Collectors.joining(","));
						csvWriter.write(lineStr + "\n");
					}
					
					csvWriter.flush();
				}
			} catch (IOException e) {
				throw new PersistenceException(e);
			}
		};

		new Thread(dataCopier, "Data Stream Copier").start();
		return input;
	}
	
	private void deletePlateData(Plate plate) {
		String stmt = String.format("delete from %s.%s where well_id in (select well_id from %s.%s where plate_id = %d)",
				SCHEMA, DATA_TABLE, SCHEMA, "hca_plate_well", plate.getId());
		try (Connection conn = getConnection()) {
			executeStatement(stmt, conn);
		} catch (SQLException e) {
			EclipseLog.error(String.format("Failed to delete subwelldata for %s", plate), e, Platform.getBundle(Activator.class.getPackage().getName()));
		}
	}
	
	private Well getWell(List<Well> wells, long well_id) {
		return wells.stream().filter(w -> w.getId() == well_id).findAny().orElse(null);
	}

	private Map<SubWellFeature, Integer> getFeatureMapping(ProtocolClass pc) {
		Map<SubWellFeature, Integer> features = new HashMap<>();

		// Get current mappings.
		String sql = String.format("select feature_id, sequence_nr from %s.%s where protocolclass_id = %d", SCHEMA, MAPPING_TABLE, pc.getId());
		long[] mapping = select(sql, rs -> {
			long[] m = new long[MAX_FEATURES];
			while (rs.next()) m[rs.getInt(2)] = rs.getLong(1);
			return m;
		});
		
		for (SubWellFeature f: pc.getSubWellFeatures()) {
			int featureIndex = getFeatureIndex(f.getId(), mapping);
			
			if (featureIndex == -1) {
				// No mapping yet, find a free spot to create a mapping.
				featureIndex = getFeatureIndex(0, mapping);
				//TODO Recycle old mappings. This will start failing after MAX_FEATURES.
				if (featureIndex == -1) throw new RuntimeException("Failed to create feature mapping: too many mappings defined");
				
				createFeatureMapping(f, featureIndex);
				mapping[featureIndex] = f.getId();
			}
			
			features.put(f, featureIndex);
		}
		
		return features;
	}
	
	private int getFeatureIndex(long featureId, long[] mapping) {
		int featureIndex = -1;
		for (int i = 0; i < mapping.length; i++) {
			if (mapping[i] == featureId) { featureIndex = i; break; }
		}
		return featureIndex;
	}
	
	private void createFeatureMapping(SubWellFeature feature, int index) {
		String queryString = String.format("insert into %s.%s (protocolclass_id,feature_id,sequence_nr) values (%d,%d,%d)",
				SCHEMA, MAPPING_TABLE, feature.getProtocolClass().getId(), feature.getId(), index);
		try (Connection conn = getConnection()) {
			try (Statement stmt = conn.createStatement()) {
				stmt.executeUpdate(queryString);
			}
			conn.commit();
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

	private String getColNames(Map<SubWellFeature, Integer> featureMapping) {
		return  "well_id,cell_id," + featureMapping.keySet().stream()
			.map(f -> String.format("f%d_num_val", featureMapping.get(f)))
			.collect(Collectors.joining(","));	
	}

	private interface ResultProcessor<T> {
		public T process(ResultSet rs) throws SQLException;
	}
	
	private Connection getConnection() throws SQLException {
		return Screening.getEnvironment().getJDBCConnection();
	}
}
