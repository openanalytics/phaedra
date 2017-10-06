package eu.openanalytics.phaedra.model.subwell.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.persistence.PersistenceException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;

import eu.openanalytics.phaedra.base.db.pool.ConnectionPoolManager;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;
import eu.openanalytics.phaedra.model.protocol.vo.SubWellFeature;
import eu.openanalytics.phaedra.model.subwell.Activator;
import eu.openanalytics.phaedra.model.subwell.cache.SubWellDataCache;

public class DBDataSource implements ISubWellDataSource {

	private ConnectionPoolManager connectionPoolManager;

	public DBDataSource() {
		//TODO configure
//		String username = "monetdb";
//		String password = "monetdb";
//		String baseURL = "jdbc:monetdb://localhost/phaedra";
		
		try { Class.forName("org.postgresql.Driver"); } catch (ClassNotFoundException e) {}
//		String username = "phaedra_admin";
//		String password = "pahedra$582";
//		String baseURL = "jdbc:postgresql://phaedra.c4rbrvxxyoqu.eu-west-1.rds.amazonaws.com:5432/phaedra";
		String baseURL = "jdbc:postgresql://localhost:5432/phaedra";
		String username = "phaedra";
		String password = "phaedra";
		
		connectionPoolManager = new ConnectionPoolManager(baseURL, username, password);
		try {
			connectionPoolManager.startup();
		} catch (SQLException e) {
			throw new RuntimeException("Failed to open database connection", e);
		}
	}

	@Override
	public void close() {
		try { connectionPoolManager.close(); } catch (Exception e) {}
	}

	@Override
	public int getNrCells(Well well) {
		String sql = String.format("select count(*) from phaedra.hca_cell where wellId = %d", well.getId());
		return select(sql, rs -> {
			rs.next();
			return rs.getInt(1);
		});
	}

	@Override
	public float[] getNumericData(Well well, SubWellFeature feature) {
		String sql = String.format("select f%dNumVal from phaedra.hca_cell where wellId = %d order by cellId asc", getFeatureIndex(feature), well.getId());
		return select(sql, rs -> {
			float[] values = new float[10000];
			int i=0;
			while (rs.next()) values[i++] = rs.getFloat(1);
			return Arrays.copyOf(values, i);
		});
	}

	@Override
	public String[] getStringData(Well well, SubWellFeature feature) {
		String sql = String.format("select f%dStrVal from phaedra.hca_cell where wellId = %d order by cellId asc", getFeatureIndex(feature), well.getId());
		return select(sql, rs -> {
			String[] values = new String[10000];
			int i=0;
			while (rs.next()) values[i++] = rs.getString(1);
			return Arrays.copyOf(values, i);
		});
	}

	@Override
	public void preloadData(List<Well> wells, List<SubWellFeature> features, SubWellDataCache cache, IProgressMonitor monitor) {
		String wellIds = wells.stream().map(w -> String.valueOf(w.getId())).collect(Collectors.joining(","));

		// First, retrieve the cell count of each well.
		Map<Well, Integer> cellCounts = new HashMap<>();
		String sql = String.format("select wellId, count(*) as cellCount from phaedra.hca_cell where wellId in (%s) group by wellId", wellIds);
		select(sql, rs -> {
			while (rs.next()) {
				Well well = getWell(wells, rs.getLong("wellId"));
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
		sql = String.format("select %s from phaedra.hca_cell where wellId in (%s) order by wellId asc", colNames, wellIds);
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
			long wellId = rs.getLong("wellId");
			int cellId = rs.getInt("cellId");

			if (currentWell == null || wellId != currentWell.getId()) {
				addToCache(currentWell, currentWellData, cache);
				currentWell = getWell(wells, wellId);
				currentWellData.clear();
			}

			int cellCount = cellCounts.get(currentWell);
			for (SubWellFeature feature: features) {
				int index = featureIndices.get(feature);
				if (feature.isNumeric()) {
					float value = rs.getFloat(String.format("f%dNumVal", index));
					if (!currentWellData.containsKey(feature)) currentWellData.put(feature, new float[cellCount]); 
					float[] values = (float[]) currentWellData.get(feature);
					values[cellId] = value;
				} else {
					String value = rs.getString(String.format("f%dStrVal", index));
					if (!currentWellData.containsKey(feature)) currentWellData.put(feature, new String[cellCount]); 
					String[] values = (String[]) currentWellData.get(feature);
					values[cellId] = value;
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
			String colName = String.format(feature.isNumeric() ? "f%dNumVal" : "f%dStrVal", getFeatureIndex(feature));
			String queryString = String.format("update phaedra.hca_cell set %s = ? where wellId = ? and cellId = ?", colName);

			Map<Well, Object> featureData = data.get(feature);

			PreparedStatement ps = null;
			try (Connection conn = connectionPoolManager.getConnection()) {
				ps = conn.prepareStatement(queryString);

				if (feature.isNumeric()) {
					for (Well well: featureData.keySet()) {
						float[] numVal = (float[]) featureData.get(well);
						for (int cellId = 0; cellId < numVal.length; cellId++) {
							ps.setFloat(1, numVal[cellId]);
							ps.setLong(2, well.getId());
							ps.setLong(3, cellId);
							ps.addBatch();
						}
					}
				} else {
					for (Well well: featureData.keySet()) {
						String[] strVal = (String[]) featureData.get(well);
						for (int cellId = 0; cellId < strVal.length; cellId++) {
							ps.setString(1, strVal[cellId]);
							ps.setLong(2, well.getId());
							ps.setLong(3, cellId);
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

	private Well getWell(List<Well> wells, long wellId) {
		return wells.stream().filter(w -> w.getId() == wellId).findAny().orElse(null);
	}

	private int getFeatureIndex(SubWellFeature feature) {
		//TODO Implement proper feature index mapping. This will break when features are added/removed/renamed in the protocolclass.
		List<SubWellFeature> features = ProtocolUtils.getSubWellFeatures(feature.getProtocolClass());
		Collections.sort(features, ProtocolUtils.FEATURE_NAME_SORTER);
		return features.indexOf(feature);
	}

	private <T> T select(String sql, ResultProcessor<T> resultProcessor) {
		long start = System.currentTimeMillis();
		try (Connection conn = connectionPoolManager.getConnection()) {
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

	private String getColNames(Collection<SubWellFeature> features) {
		return  "wellId,cellId," + features.stream()
			.map(f -> String.format(f.isNumeric() ? "f%dNumVal" : "f%dStrVal", getFeatureIndex(f)))
			.collect(Collectors.joining(","));	
	}

	private interface ResultProcessor<T> {
		public T process(ResultSet rs) throws SQLException;
	}
}
