package eu.openanalytics.phaedra.model.curve.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import javax.persistence.Query;

import eu.openanalytics.phaedra.base.db.JDBCUtils;
import eu.openanalytics.phaedra.base.environment.Screening;
import eu.openanalytics.phaedra.base.util.CollectionUtils;
import eu.openanalytics.phaedra.model.curve.CurveFitService;
import eu.openanalytics.phaedra.model.curve.CurveFitSettings;
import eu.openanalytics.phaedra.model.curve.CurveParameter;
import eu.openanalytics.phaedra.model.curve.CurveParameter.Value;
import eu.openanalytics.phaedra.model.curve.util.CurveUtils;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;

public class CustomSettingsDAO {

private EntityManager em;

	public CustomSettingsDAO(EntityManager em) {
		this.em = em;
	}

	public List<CustomCurveSettings> loadSettings(Plate plate) {
		List<CustomCurveSettings> returnValue = new ArrayList<>();
		
		String queryString = "select cs.curve_id, c.feature_id, cs.setting_name, cs.setting_value"
				+ " from phaedra.hca_curve_setting_custom cs, phaedra.hca_curve_compound cc, phaedra.hca_plate_compound pc, phaedra.hca_curve c"
				+ " where cs.curve_id = cc.curve_id and cs.curve_id = c.curve_id and cc.platecompound_id = pc.platecompound_id"
				+ " and pc.plate_id = " + plate.getId()
				+ " order by curve_id";
		
		Query query = em.createNativeQuery(queryString);
		List<?> resultSet = JDBCUtils.queryWithLock(query, em);
		if (resultSet == null || resultSet.isEmpty()) return returnValue;

		Map<Long, List<CustomSettingRecord>> records = resultSet.stream()
			.map(r -> {
				Object[] row = (Object[]) r;
				return new CustomSettingRecord(((Number)row[0]).longValue(), ((Number)row[1]).longValue(), (String) row[2], (String) row[3]);	
			})
			.collect(Collectors.groupingBy(r -> r.curveId));

		List<Feature> features = CollectionUtils.findAll(ProtocolUtils.getFeatures(plate), CurveUtils.FEATURES_WITH_CURVES);
		Map<Long, CurveFitSettings> featureSettings = new HashMap<>();
		for (Feature feature: features) {
			featureSettings.put(feature.getId(), CurveFitService.getInstance().getSettings(feature));
		}
		
		for (long curveId: records.keySet()) {
			List<CustomSettingRecord> curveRecords = records.get(curveId);
			if (curveRecords.isEmpty()) continue;
			
			CustomCurveSettings settings = new CustomCurveSettings();
			settings.curveId = curveId;
			settings.settings = new CurveFitSettings();
			
			CurveFitSettings defaults = featureSettings.get(curveRecords.get(0).featureId);
			settings.settings.setModelId(defaults.getModelId());
			settings.settings.setGroupingFeatures(defaults.getGroupingFeatures());

			List<Value> customParams = new ArrayList<>();
			for (CustomSettingRecord rec: curveRecords) {
				Value value = Arrays.stream(defaults.getExtraParameters())
						.filter(v -> v.definition.name.equals(rec.name))
						.map(v -> CurveParameter.createValue(rec.value, v.definition))
						.findAny().orElse(null);
				if (value != null) customParams.add(value);
				// Special case: modelId can be overridden but it's not an 'extra parameter'.
				else if (rec.name.equals(CurveFitSettings.MODEL)) settings.settings.setModelId(rec.value);
			}
			settings.settings.setExtraParameters(customParams.toArray(new Value[customParams.size()]));
			returnValue.add(settings);
		}
		return returnValue;
	}

	public void saveSettings(long curveId, CurveFitSettings settings) {
		PreparedStatement ps = null;
		try (Connection conn = Screening.getEnvironment().getJDBCConnection()) {
			ps = conn.prepareStatement("insert into phaedra.hca_curve_setting_custom (curve_id, setting_name, setting_value) values(?, ?, ?)");
			ps.setLong(1, curveId);
			ps.setString(2, CurveFitSettings.MODEL);
			ps.setString(3, settings.getModelId());
			ps.addBatch();
			for (int i = 0; i < settings.getExtraParameters().length; i++) {
				Value v = settings.getExtraParameters()[i];
				ps.setLong(1, curveId);
				ps.setString(2, v.definition.name);
				ps.setString(3, CurveParameter.getValueAsString(v));
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
		public CurveFitSettings settings;
	}
	
	private static class CustomSettingRecord {
		public long curveId;
		public long featureId;
		public String name;
		public String value;
		
		public CustomSettingRecord(long curveId, long featureId, String name, String value) {
			this.curveId = curveId;
			this.featureId = featureId;
			this.name = name;
			this.value = value;
		}
	}

}
