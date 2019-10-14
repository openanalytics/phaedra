package eu.openanalytics.phaedra.calculation.outlier;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.stream.Collectors;

import javax.persistence.PersistenceException;

import eu.openanalytics.phaedra.base.environment.Screening;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;

public class OutlierValueDAO {

	// Note: values are returned in ascending wellId order
	public double[] getOutlierValues(long[] wellIds, long featureId) {
		String wellIdArg = Arrays.stream(wellIds).mapToObj(l -> String.valueOf(l)).collect(Collectors.joining(","));
		String sql = "select outlier_value"
				+ " from phaedra.hca_outlier_detection_value"
				+ " where feature_id = " + featureId
				+ " and well_id in (" + wellIdArg + ")"
				+ " order by well_id asc";
		double[] outlierValues = new double[wellIds.length];
		
		long startTime = System.currentTimeMillis();
		try (Connection conn = getConnection()) {
			ResultSet resultSet = conn.createStatement().executeQuery(sql);
			int index = 0;
			while (resultSet.next()) {
				double value = resultSet.getDouble(1);
				outlierValues[index++] = value;
			}
		} catch (SQLException e) {
			throw new PersistenceException(e);
		}
		long duration = System.currentTimeMillis() - startTime;
		EclipseLog.debug(String.format("Query in %d ms [%d wells, featureId: %d]", duration, wellIds.length, featureId), OutlierValueDAO.class);
		
		return outlierValues;
	}
	
	//TODO Optimize upsert, at least for postgres
	public void saveOutlierValues(long[] wellIds, long featureId, double[] outlierValues) {
		if (wellIds == null || outlierValues == null || wellIds.length != outlierValues.length) {
			throw new IllegalArgumentException("Invalid wellIds or outlierValues argument");
		}
		
		String sqlCheck = String.format("select count(*) from phaedra.hca_outlier_detection_value where well_id in (%s) and feature_id = %d", getWellIdArg(wellIds), featureId);
		String sqlDelete = String.format("delete from phaedra.hca_outlier_detection_value where well_id in (%s) and feature_id = %d", getWellIdArg(wellIds), featureId);
		String sqlInsert = "insert into phaedra.hca_outlier_detection_value (well_id, feature_id, outlier_value) values (?,?,?)";
		
		long startTime = System.currentTimeMillis();
		try (Connection conn = getConnection()) {
			int existingValueCount = 0;
			try (Statement stmt = conn.createStatement()) {
				ResultSet rs = stmt.executeQuery(sqlCheck);
				rs.next();
				existingValueCount = rs.getInt(1);
			}
			if (existingValueCount > 0) {
				try (Statement stmt = conn.createStatement()) {
					stmt.execute(sqlDelete);
				}
			}
			try (PreparedStatement ps = conn.prepareStatement(sqlInsert)) {
				for (int i = 0; i < outlierValues.length; i++) {
					ps.setLong(1, wellIds[i]);
					ps.setLong(2, featureId);
					ps.setDouble(3, outlierValues[i]);
					ps.addBatch();
				}
				ps.executeBatch();
			}
			conn.commit();
		} catch (SQLException e) {
			throw new PersistenceException(e);
		}
		long duration = System.currentTimeMillis() - startTime;
		EclipseLog.debug(String.format("Updated %d values in %s ms", outlierValues.length, duration), OutlierValueDAO.class);
	}
	
	private String getWellIdArg(long[] wellIds) {
		return Arrays.stream(wellIds).mapToObj(l -> String.valueOf(l)).collect(Collectors.joining(","));
	}
	
	private Connection getConnection() {
		return Screening.getEnvironment().getJDBCConnection();
	}
}
