package eu.openanalytics.phaedra.silo.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.persistence.PersistenceException;

import eu.openanalytics.phaedra.base.environment.Screening;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.base.util.misc.StringUtils;
import eu.openanalytics.phaedra.silo.dao.SiloDatasetData.SiloDatapoint;
import eu.openanalytics.phaedra.silo.dao.SiloDatasetData.SiloDatasetColumnData;
import eu.openanalytics.phaedra.silo.util.SiloUtils;
import eu.openanalytics.phaedra.silo.vo.SiloDataset;
import eu.openanalytics.phaedra.silo.vo.SiloDatasetColumn;

public class SiloDataDAO {

	public SiloDatasetData loadData(SiloDataset dataset) {
		SiloDatasetData data = new SiloDatasetData();
		data.setDataset(dataset);
		
		// Query all datapoints
		List<SiloDatapoint> points = queryDataPoints(dataset.getId());
		data.setDataPoints(points.toArray(new SiloDatapoint[points.size()]));
		
		// Query all additional columns
		String sql = "select dpv.datapoint_id, dpv.column_id, dpv.str_value, dpv.float_value, dpv.long_value"
				+ " from phaedra.hca_silo_datapoint_value dpv, phaedra.hca_silo_datapoint dp"
				+ " where dpv.datapoint_id = dp.datapoint_id"
				+ " and dp.dataset_id = " + dataset.getId()
				+ " order by dp.datapoint_id asc";
		runQuery(sql, new DatasetColumnDataMapper(data));
		return data;
	}
	
	public void saveData(SiloDatasetData data) {
		long datasetId = data.getDataset().getId();
		
		// Identify new points and insert them.
		List<SiloDatapoint> newPoints = Arrays.stream(data.getDataPoints()).filter(p -> p.getDatapointId() == 0).collect(Collectors.toList());
		//TODO Support other DB's sequence syntax
		String sql = "insert into phaedra.hca_silo_datapoint(datapoint_id,dataset_id,well_id,subwell_id)"
				+ " values (nextval('phaedra.hca_silo_datapoint_s'),?,?,?)";
		runBatch(sql, newPoints, (p, ps) -> {
			ps.setLong(1, datasetId);
			ps.setLong(2, p.getWellId());
			ps.setLong(3, p.getSubwellId());
		});
		
		// Identify removed points and delete them.
		long[] currentPointIds = Arrays.stream(data.getDataPoints()).mapToLong(p -> p.getDatapointId()).sorted().toArray();
		String[] removedPoints = queryDataPoints(datasetId).stream()
				.filter(p -> Arrays.binarySearch(currentPointIds, p.getDatapointId()) < 0)
				.map(p -> String.valueOf(p.getDatapointId()))
				.toArray(i -> new String[i]);
		if (removedPoints.length > 0) {
			runStatement("delete from phaedra.hca_silo_datapoint where datapoint_id in (" 
				+ StringUtils.createSeparatedString(removedPoints, ",") + ")");
		}
		
		for (SiloDatasetColumnData colData: data.getColumnData().values()) {
			runStatement("delete from phaedra.hca_silo_datapoint_value"
					+ " where column_id = " + colData.getColumn().getId()
					+ " and datapoint_id in (select datapoint_id from phaedra.hca_silo_datapoint where dataset_id = " + datasetId + ")");
			
			sql = "insert into phaedra.hca_silo_datapoint_value(datapoint_id,column_id,str_value,float_value,long_value)"
					+ " values (?,?,?,?,?)";
			List<Integer> indices = IntStream.range(0, data.getDataPoints().length).mapToObj(i -> i).collect(Collectors.toList());
			runBatch(sql, indices, (i, ps) -> {
				SiloDatapoint p = data.getDataPoints()[i];
				String strVal = colData.getStringData() == null ? null : colData.getStringData()[i];
				Float floatVal = colData.getStringData() == null ? null : colData.getFloatData()[i];
				Long longVal = colData.getStringData() == null ? null : colData.getLongData()[i];
				
				ps.setLong(1, p.getDatapointId());
				ps.setLong(2, colData.getColumn().getId());
				ps.setString(3, strVal);
				ps.setObject(4, floatVal);
				ps.setObject(5, longVal);
			});
		}
	}
	
	public void deleteData(long datasetId) {
		String sql = "delete from phaedra.hca_silo_dataset where dataset_id = " + datasetId;
		runStatement(sql);
	}
	
	private List<SiloDatapoint> queryDataPoints(long datasetId) {
		// Query all datapoints
		String sql = "select dp.datapoint_id, dp.well_id, dp.subwell_id"
				+ " from phaedra.hca_silo_datapoint dp"
				+ " where dp.dataset_id = " + datasetId
				+ " order by dp.datapoint_id asc";
		List<SiloDatapoint> points = runQuery(sql, rs -> {
			SiloDatapoint p = new SiloDatapoint();
			p.setDatapointId(rs.getLong(1));
			p.setWellId(rs.getLong(2));
			p.setSubwellId(rs.getLong(3));
			return p;
		});
		return points;
	}
	
	private <E> List<E> runQuery(String sql, ResultSetMapper<E> mapper) {
		long startTime = System.currentTimeMillis();

		List<E> results = new ArrayList<>();
		try (Connection conn = getConnection()) {
			ResultSet resultSet = conn.createStatement().executeQuery(sql);
			if (resultSet.isBeforeFirst()) {
				while (resultSet.next()) {
					E value = mapper.map(resultSet);
					results.add(value);
				}
			}
		} catch (SQLException e) {
			throw new PersistenceException(e);
		}

		long duration = System.currentTimeMillis() - startTime;
		EclipseLog.debug("Query executed in " + duration + "ms", SiloDataDAO.class);
		return results;
	}
	
	private void runStatement(String sql) {
		long startTime = System.currentTimeMillis();
		try (Connection conn = getConnection()) {
			conn.createStatement().execute(sql);
			conn.commit();
		} catch (SQLException e) {
			throw new PersistenceException(e);
		}
		long duration = System.currentTimeMillis() - startTime;
		EclipseLog.debug("Statement executed in " + duration + "ms", SiloDataDAO.class);
	}
	
	private <E> void runBatch(String sql, List<E> objects, PreparedStatementMapper<E> objectMapper) {
		if (objects == null || objects.isEmpty()) return;
		
		long startTime = System.currentTimeMillis();
		PreparedStatement ps = null;
		try (Connection conn = getConnection()) {
			ps = conn.prepareStatement(sql);
			
			for (E object: objects) {
				objectMapper.map(object, ps);
				ps.addBatch();
			}
			
			ps.executeBatch();
			conn.commit();
		} catch (SQLException e) {
			throw new PersistenceException(e);
		} finally {
			if (ps != null) try { ps.close(); } catch (SQLException e) {}
		}
		long duration = System.currentTimeMillis() - startTime;
		EclipseLog.debug("Batch executed in " + duration + "ms", SiloDataDAO.class);
	}
	
	private Connection getConnection() {
		return Screening.getEnvironment().getJDBCConnection();
	}
	
	private static interface ResultSetMapper<E> {
		public E map(ResultSet rs) throws SQLException;
	}
	
	private static interface PreparedStatementMapper<E> {
		public void map(E object, PreparedStatement ps) throws SQLException;
	}
	
	private static class DatasetColumnDataMapper implements ResultSetMapper<Object> {
		
		private SiloDatasetData data;
		private int pointIndex;
		
		public DatasetColumnDataMapper(SiloDatasetData data) {
			this.data = data;
			this.pointIndex = 0;
		}
		
		@Override
		public Object map(ResultSet rs) throws SQLException {
			long dpId = rs.getLong(1);
			long colId = rs.getLong(2);
			String strVal = rs.getString(3);
			Double floatVal = (Double) rs.getObject(4);
			Number longVal = (Number) rs.getObject(5);
			
			int dsSize = data.getDataPoints().length;
			SiloDatapoint point = data.getDataPoints()[pointIndex];
			while (point.getDatapointId() != dpId && pointIndex < dsSize) {
				pointIndex++;
				point = data.getDataPoints()[pointIndex];
			}
			if (pointIndex == dsSize) {
				throw new RuntimeException("Column data found for nonexisting data point: " + dpId);
			}
			
			SiloDatasetColumn column = SiloUtils.getColumn(data.getDataset(), colId);
			SiloDatasetColumnData columnData = data.getColumnData().get(column.getName());
			if (columnData == null) {
				columnData = new SiloDatasetColumnData();
				columnData.setColumn(column);
				data.getColumnData().put(column.getName(), columnData);
			}
			
			if (strVal != null) {
				String[] strData = columnData.getStringData();
				if (strData == null) {
					strData = new String[dsSize];
					columnData.setStringData(strData);
				}
				strData[pointIndex] = strVal;
			}
			if (floatVal != null) {
				float[] floatData = columnData.getFloatData();
				if (floatData == null) {
					floatData = new float[dsSize];
					columnData.setFloatData(floatData);
				}
				floatData[pointIndex] = floatVal.floatValue();
			}
			if (longVal != null) {
				long[] longData = columnData.getLongData();
				if (longData == null) {
					longData = new long[dsSize];
					columnData.setLongData(longData);
				}
				longData[pointIndex] = longVal.longValue();
			}
			
			return null;
		}
		
	}
}
