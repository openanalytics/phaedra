package eu.openanalytics.phaedra.datacapture.metrics.internal.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import javax.persistence.Query;

import eu.openanalytics.phaedra.base.db.JDBCUtils;
import eu.openanalytics.phaedra.base.environment.Screening;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.datacapture.metrics.model.ServerMetric;

public class ServerMetricDAO {

	private EntityManager em;

	public ServerMetricDAO(EntityManager em) {
		this.em = em;
	}

	public List<ServerMetric> getValues(Date from, Date to) {
		if(from.getTime() > to.getTime()) {
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

		Query query = em.createNativeQuery(queryString);
		query.setParameter(1, from);
		query.setParameter(2, to);

		List<?> resultSet = JDBCUtils.queryWithLock(query, em);
		List<ServerMetric> values = mapValues(resultSet);

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

	private List<ServerMetric> mapValues(List<?> resultSet) {
		List<ServerMetric> values = new ArrayList<ServerMetric>();

		for (Object o: resultSet) {
			Object[] row = (Object[])o;
			long metricId = ((Number)row[0]).longValue();
			long timestamp = ((Timestamp)row[1]).getTime();
			long diskUsage = ((Number)row[2]).longValue();
			long ramUsage = ((Number)row[3]).longValue();
			Double cpu = ((Number)row[4]).doubleValue();
			long downloadSpeed = ((Number)row[5]).longValue();
			long uploadSpeed = ((Number)row[6]).longValue();

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

}
