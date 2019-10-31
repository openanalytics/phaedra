package eu.openanalytics.phaedra.datacapture.metrics.internal.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.PersistenceException;

import eu.openanalytics.phaedra.base.environment.Screening;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.datacapture.metrics.model.ServerMetric;

public class ServerMetricDAO {

	public List<ServerMetric> getValues(Date from, Date to) {
		if (from.getTime() > to.getTime()) {
			Date temp = from;
			from = to;
			to = temp;
		}

		long startTime = System.currentTimeMillis();
		String queryString = "select m.metric_id, m.timestamp,"
				+ " m.disk_usage, m.ram_usage, m.cpu_usage,"
				+ " m.dl_speed, m.ul_speed"
				+ " from phaedra.hca_dc_metric m"
				+ " where m.timestamp > ?"
				+ " and m.timestamp < ?";
		
		List<ServerMetric> values = null;
		
		try (Connection conn = getConnection()) {
			try (PreparedStatement stmt = conn.prepareStatement(queryString)) {
				stmt.setObject(1, from);
				stmt.setObject(2, to);
				ResultSet rs = stmt.executeQuery();
				values = mapValues(rs);
			}
		} catch (SQLException e) {
			throw new PersistenceException(e);
		}

		long duration = System.currentTimeMillis() - startTime;
		EclipseLog.debug("ServerMetric lookup in " + duration + "ms"
				+ " [from " + from.toString()
				+ " to " + to.toString() + "]", ServerMetricDAO.class);
		return values;
	}

	public void insertValue(ServerMetric serverMetric) {
		long startTime = System.currentTimeMillis();

		PreparedStatement ps = null;
		try (Connection conn = Screening.getEnvironment().getJDBCConnection()) {
			ps = conn.prepareStatement(
					"insert into phaedra.hca_dc_metric m"
						+ " (metric_id,"
						+ " timestamp,"
						+ " disk_usage,"
						+ " ram_usage,"
						+ " cpu_usage,"
						+ " dl_speed,"
						+ " ul_speed) values(phaedra.hca_dc_metric_s.nextval,?,?,?,?,?,?)");
			ps.setTimestamp(1, new java.sql.Timestamp(serverMetric.getTimestamp().getTime()));
			ps.setLong(2, serverMetric.getDiskUsage());
			ps.setLong(3, serverMetric.getRamUsage());
			ps.setDouble(4, serverMetric.getCpu());
			ps.setLong(5, serverMetric.getDownloadSpeed());
			ps.setLong(6, serverMetric.getUploadSpeed());

			ps.execute();
			conn.commit();
		} catch (SQLException e) {
			throw new PersistenceException(e);
		} finally {
			if (ps != null) try { ps.close(); } catch (SQLException e) {}
		}

		long duration = System.currentTimeMillis() - startTime;
		EclipseLog.debug("ServerMetric update in " + duration + "ms", ServerMetricDAO.class);
	}

	/*
	 * **********
	 * Non-public
	 * **********
	 */

	private List<ServerMetric> mapValues(ResultSet rs) throws SQLException {
		List<ServerMetric> values = new ArrayList<ServerMetric>();

		while (rs.next()) {
			long metricId = rs.getLong(1);
			long timestamp = rs.getTimestamp(2).getTime();
			long diskUsage = rs.getLong(3);
			long ramUsage = rs.getLong(4);
			Double cpu = (Double) rs.getObject(5);
			long downloadSpeed = rs.getLong(6);
			long uploadSpeed = rs.getLong(7);

			ServerMetric serverMetric = new ServerMetric();
			serverMetric.setId(metricId);
			serverMetric.setTimestamp(new Date(timestamp));
			serverMetric.setDiskUsage(diskUsage);
			serverMetric.setRamUsage(ramUsage);
			serverMetric.setCpu(cpu);
			serverMetric.setDownloadSpeed(downloadSpeed);
			serverMetric.setUploadSpeed(uploadSpeed);
			serverMetric.setId(metricId);

			values.add(serverMetric);
		}

		return values;
	}

	private Connection getConnection() {
		return Screening.getEnvironment().getJDBCConnection();
	}
}
