package eu.openanalytics.phaedra.calculation.stat.persist;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.persistence.PersistenceException;

import org.w3c.dom.Document;

import eu.openanalytics.phaedra.base.db.JDBCUtils;
import eu.openanalytics.phaedra.base.environment.Screening;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.base.util.xml.XmlUtils;
import eu.openanalytics.phaedra.calculation.Activator;
import eu.openanalytics.phaedra.model.plate.vo.Plate;

public class PersistentStatAccessor {

	public PersistentPlateStats getStoredStats(Plate plate) {
		try {
			return queryStoredStats(plate);
		} catch (IOException e) {
			throw new RuntimeException("Failed to load persistent stats", e);
		}
	}

	public void saveStoredStats(Plate plate, PersistentPlateStats stats) {
		String xml = queryPlateXML(plate);
		try {
			Document doc = null;
			if (xml == null) doc = XmlUtils.createEmptyDoc();
			else doc = XmlUtils.parse(xml);
			stats.writeToXml(doc);
			xml = XmlUtils.writeToString(doc);
		} catch (IOException e) {
			throw new RuntimeException("Failed to generate persistent stats", e);
		}

		writePlateXML(plate, xml);
	}

	/*
	 * **********
	 * Non-public
	 * **********
	 */

	private PersistentPlateStats queryStoredStats(Plate plate) throws IOException {
		PersistentPlateStats stats = null;
		String xml = queryPlateXML(plate);
		if (xml != null) {
			stats = new PersistentPlateStats();
			stats.loadFromXml(xml);
		}
		return stats;
	}

	private String queryPlateXML(Plate plate) {
		String sql = "select " + JDBCUtils.selectXMLColumn("p.data_xml") + " from phaedra.hca_plate p where p.plate_id = " + plate.getId();

		PreparedStatement ps = null;
		try (Connection conn = Screening.getEnvironment().getJDBCConnection()) {
			ps = conn.prepareStatement(sql);
			ResultSet resultSet = ps.executeQuery();
			if (resultSet.next()) return resultSet.getString(1);
		} catch (SQLException e) {
			EclipseLog.error(e.getMessage(), e, Activator.getDefault());
		} finally {
			if (ps != null) try { ps.close(); } catch (SQLException e) {};
		}

		return null;
	}
	
	private void writePlateXML(Plate plate, String xml) {
		try (Connection conn = Screening.getEnvironment().getJDBCConnection()) {
			String queryString = "update phaedra.hca_plate p set " + JDBCUtils.updateXMLColumn("data_xml") + " where p.plate_id = ?";
			try (PreparedStatement stmt = conn.prepareStatement(queryString)) {
				stmt.setObject(1, JDBCUtils.getXMLObjectParameter(xml, conn));
				stmt.setLong(2, plate.getId());
				stmt.execute();
				conn.commit();
			}
		} catch (SQLException e) {
			throw new PersistenceException(e);
		}
	}
}
