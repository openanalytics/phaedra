package eu.openanalytics.phaedra.model.curve.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import javax.persistence.Query;

import eu.openanalytics.phaedra.base.db.JDBCUtils;
import eu.openanalytics.phaedra.base.environment.Screening;
import eu.openanalytics.phaedra.model.curve.util.CurveSettingsMapper;
import eu.openanalytics.phaedra.model.curve.vo.CurveSettings;

public class CustomSettingsDAO {

private EntityManager em;

	public CustomSettingsDAO(EntityManager em) {
		this.em = em;
	}

	public CustomCurveSettings[] loadSettings(long plateId) {
		String queryString = "select cs.curve_id, cs.setting_name, cs.setting_value"
				+ " from phaedra.hca_curve_setting_custom cs, phaedra.hca_curve_compound cc, phaedra.hca_plate_compound pc"
				+ " where cs.curve_id = cc.curve_id and cc.platecompound_id = pc.platecompound_id and pc.plate_id = " + plateId
				+ " order by curve_id";
		
		Query query = em.createNativeQuery(queryString);
		List<?> resultSet = JDBCUtils.queryWithLock(query, em);
		if (resultSet == null || resultSet.isEmpty()) return new CustomCurveSettings[0];

		List<CustomCurveSettings> settingsList = new ArrayList<>();
		long previousCurveId = 0;

		CustomCurveSettings csettings = new CustomCurveSettings();
		csettings.settings = new CurveSettings();

		for (Object o: resultSet) {
			Object[] row = (Object[])o;

			long curveId = ((Number) row[0]).longValue();
			String name = (String) row[1];
			String value = (String) row[2];

			if (previousCurveId != 0 && curveId != previousCurveId) {
				csettings.curveId = previousCurveId;
				settingsList.add(csettings);
				csettings = new CustomCurveSettings();
				csettings.settings = new CurveSettings();
			}

			CurveSettingsMapper.addSetting(name, value, csettings.settings);

			previousCurveId = curveId;
		}

		csettings.curveId = previousCurveId;
		settingsList.add(csettings);

		return settingsList.toArray(new CustomCurveSettings[settingsList.size()]);
	}

	public void saveSettings(long curveId, CurveSettings settings) {
		PreparedStatement ps = null;
		try (Connection conn = Screening.getEnvironment().getJDBCConnection()) {
			ps = conn.prepareStatement("insert into phaedra.hca_curve_setting_custom (curve_id, setting_name, setting_value) values(?, ?, ?)");

			Map<String,String> map = CurveSettingsMapper.toMap(settings);
			for (String key: map.keySet()) {
				ps.setLong(1, curveId);
				ps.setString(2, key);
				ps.setString(3, map.get(key));
				ps.addBatch();
			}

			ps.executeBatch();
			conn.commit();
		} catch (SQLException e) {
			throw new PersistenceException(e);
		} finally {
			if (ps != null) try { ps.close(); } catch (SQLException e) {}
		}
	}

	public void clearSettings(long curveId) {
		Statement stmt = null;
		try (Connection conn = Screening.getEnvironment().getJDBCConnection()) {
			stmt = conn.createStatement();
			stmt.execute("delete from phaedra.hca_curve_setting_custom where curve_id = " + curveId);
			conn.commit();
		} catch (SQLException e) {
			throw new PersistenceException(e);
		} finally {
			if (stmt != null) try { stmt.close(); } catch (SQLException e) {}
		}
	}

	public static class CustomCurveSettings {
		public long curveId;
		public CurveSettings settings;
	}

}
