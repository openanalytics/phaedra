package eu.openanalytics.phaedra.model.curve.dao;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import javax.persistence.Query;

import eu.openanalytics.phaedra.base.db.JDBCUtils;
import eu.openanalytics.phaedra.base.environment.Screening;
import eu.openanalytics.phaedra.base.util.misc.StringUtils;
import eu.openanalytics.phaedra.model.curve.CurveFitService;
import eu.openanalytics.phaedra.model.curve.CurveParameter;
import eu.openanalytics.phaedra.model.curve.CurveParameter.Definition;
import eu.openanalytics.phaedra.model.curve.CurveParameter.Value;
import eu.openanalytics.phaedra.model.curve.ICurveFitModel;
import eu.openanalytics.phaedra.model.curve.util.CurveGrouping;
import eu.openanalytics.phaedra.model.curve.vo.Curve;
import eu.openanalytics.phaedra.model.plate.vo.Compound;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;

public class CurveDAO {

	private static final String[] CURVE_COLUMNS = {
		"CURVE_ID","FEATURE_ID","MODEL_ID","FIT_DATE","FIT_VERSION","ERROR_CODE","GROUP_BY_1","GROUP_BY_2","GROUP_BY_3","PLOT"
	};
	
	private static final String[] CURVE_PROPERTY_COLUMNS = {
		"CURVE_ID", "PROPERTY_NAME", "NUMERIC_VALUE", "STRING_VALUE", "BINARY_VALUE"
	};
	
	private EntityManager em;

	public CurveDAO(EntityManager em) {
		this.em = em;
	}

	public Curve getCurve(long curveId) {
		String queryString = "select " + StringUtils.createSeparatedString(CURVE_COLUMNS, ",")
				+ " from phaedra.hca_curve"
				+ " where curve_id = " + curveId;
		List<?> resultSet = JDBCUtils.queryWithLock(em.createNativeQuery(queryString), em);
		if (resultSet.isEmpty()) return null;
		
		Curve curve = toCurve((Object[]) resultSet.get(0));
		setCurveCompounds(curve);
		
		queryString = "select " + StringUtils.createSeparatedString(CURVE_PROPERTY_COLUMNS, ",")
				+ " from phaedra.hca_curve_property"
				+ " where curve_id = " + curve.getId();
		resultSet = JDBCUtils.queryWithLock(em.createNativeQuery(queryString), em);
		applyProperties(curve, resultSet);
		
		return curve;
	}
	
	public Curve getCurve(Feature f, Compound c, CurveGrouping grouping) {
		String queryString = "select c." + StringUtils.createSeparatedString(CURVE_COLUMNS, ",c.", false)
				+ " from phaedra.hca_curve c, phaedra.hca_curve_compound cc"
				+ " where c.feature_id = " + f.getId()
				+ " and c.curve_id = cc.curve_id and cc.platecompound_id = " + c.getId();
		for (int i = 0; i < grouping.getCount(); i++) {
			queryString += " and c.group_by_" + (i+1) + " = '" + grouping.get(i) + "'";
		}
		List<?> resultSet = JDBCUtils.queryWithLock(em.createNativeQuery(queryString), em);
		if (resultSet.isEmpty()) return null;
		
		Curve curve = toCurve((Object[]) resultSet.get(0));
		setCurveCompounds(curve);
		
		queryString = "select " + StringUtils.createSeparatedString(CURVE_PROPERTY_COLUMNS, ",")
				+ " from phaedra.hca_curve_property"
				+ " where curve_id = " + curve.getId();
		resultSet = JDBCUtils.queryWithLock(em.createNativeQuery(queryString), em);
		applyProperties(curve, resultSet);
		
		return curve;
	}
	
	public List<Curve> getCurveBatch(Plate plate) {
		// Query 1: curves
		String queryString = "select c." + StringUtils.createSeparatedString(CURVE_COLUMNS, ",c.", false)
				+ " from phaedra.hca_curve c, phaedra.hca_curve_compound cc, phaedra.hca_plate_compound pc"
				+ " where c.curve_id = cc.curve_id and cc.platecompound_id = pc.platecompound_id and pc.plate_id =  " + plate.getId()
				+ " order by c.curve_id asc";
		List<?> resultSet = JDBCUtils.queryWithLock(em.createNativeQuery(queryString), em);
		List<Curve> curves = new ArrayList<>();
		for (Object record: resultSet) curves.add(toCurve((Object[]) record));

		// Query 2: compounds (with support for multiplo)
		queryString = "select curve_id, platecompound_id from phaedra.hca_curve_compound where curve_id in"
				+ " (select cc.curve_id from phaedra.hca_curve_compound cc, phaedra.hca_plate_compound pc"
				+ " where cc.platecompound_id = pc.platecompound_id and pc.plate_id = " + plate.getId() + ")"
				+ " order by curve_id asc";
		resultSet = JDBCUtils.queryWithLock(em.createNativeQuery(queryString), em);
		for (Object record: resultSet) {
			Object[] row = (Object[]) record;
			long curveId = ((Number) row[0]).longValue();
			long compId = ((Number) row[1]).longValue();
			Curve curve = curves.stream().filter(c -> c.getId() == curveId).findAny().orElse(null);
			curve.getCompounds().add(em.find(Compound.class, compId));
		}
		
		// Query 3: curve properties
		queryString = "select cp." + StringUtils.createSeparatedString(CURVE_PROPERTY_COLUMNS, ",cp.", false)
				+ " from phaedra.hca_curve_property cp, phaedra.hca_curve_compound cc, phaedra.hca_plate_compound pc"
				+ " where cp.curve_id = cc.curve_id and cc.platecompound_id = pc.platecompound_id and pc.plate_id = " + plate.getId()
				+ " order by cp.curve_id asc";
		resultSet = JDBCUtils.queryWithLock(em.createNativeQuery(queryString), em);
		for (Curve curve: curves) applyProperties(curve, resultSet);

		return curves;
	}
	
	public void saveCurve(Curve curve) {
		if (curve == null) return;
		deleteIncompatibleCurves(curve);
		if (curve.getId() == 0) insertCurve(curve);
		else updateCurve(curve);
	}
	
	public void deleteCurve(Curve curve) {
		if (curve == null || curve.getId() == 0) return;
		Statement stmt = null;
		try (Connection conn = Screening.getEnvironment().getJDBCConnection()) {
			stmt = conn.createStatement();
			stmt.execute("delete from phaedra.hca_curve where curve_id = " + curve.getId());
			conn.commit();
		} catch (SQLException e) {
			throw new PersistenceException(e);
		} finally {
			if (stmt != null) try { stmt.close(); } catch (SQLException e) {}
		}
	}
	
	/*
	 * Non-public
	 * **********
	 */

	private void deleteIncompatibleCurves(Curve curve) {
		String compIds = StringUtils.createSeparatedString(curve.getCompounds(), c -> "" + c.getId(), ",");
		String statement = "delete from phaedra.hca_curve c"
				+ " where c.feature_id = " + curve.getFeature().getId()
				+ " and c.curve_id != " + curve.getId()
				+ " and c.curve_id in (select cc.curve_id from phaedra.hca_curve_compound cc where cc.platecompound_id in (" + compIds + "))";
		if (curve.getGroupingValues() != null) {
			// If the curve has grouping, delete any ungrouped curves on the same compound(s).
			statement += " and c.group_by_1 is null";
		}
		
		Statement stmt = null;
		try (Connection conn = Screening.getEnvironment().getJDBCConnection()) {
			stmt = conn.createStatement();
			stmt.execute(statement);
			conn.commit();
		} catch (SQLException e) {
			throw new PersistenceException(e);
		} finally {
			if (stmt != null) try { stmt.close(); } catch (SQLException e) {}
		}
	}
	
	private long getNewCurveId() {
		return JDBCUtils.getSequenceNextVal(em, "phaedra.hca_curve_s");
	}
	
	private void setCurveCompounds(Curve curve) {
		Query query = em.createNativeQuery("select platecompound_id from phaedra.hca_curve_compound where curve_id = " + curve.getId());
		List<?> res = JDBCUtils.queryWithLock(query, em);
		for (Object o: res) {
			long id = ((Number) o).longValue();
			curve.getCompounds().add(em.find(Compound.class, id));
		}
	}
	
	private void insertCurve(Curve curve) {
		curve.setId(getNewCurveId());
		
		String[] arr = new String[CURVE_COLUMNS.length - 1];
		Arrays.fill(arr, "?");
		String statement = "insert into phaedra.hca_curve(" + StringUtils.createSeparatedString(CURVE_COLUMNS, ",") + ")"
				+ " values(" + curve.getId() + ", " + StringUtils.createSeparatedString(arr, ",") + ")";
		
		try (Connection conn = Screening.getEnvironment().getJDBCConnection()) {
			try (PreparedStatement ps = conn.prepareStatement(statement)) {
				setStatementArgs(ps, curve);
				ps.execute();
			}
			insertCurveCompounds(curve, conn);
			insertCurveProperties(curve, conn);
			conn.commit();
		} catch (SQLException e) {
			throw new PersistenceException(e);
		}
	}
	
	private void updateCurve(Curve curve) {
		String[] updateColumns = new String[CURVE_COLUMNS.length - 1];
		System.arraycopy(CURVE_COLUMNS, 1, updateColumns, 0, updateColumns.length);
		String updateColumnsString = StringUtils.createSeparatedString(updateColumns, "=?,");
		String statement = "update phaedra.hca_curve set " + updateColumnsString + "=? where curve_id = " + curve.getId();
		
		try (Connection conn = Screening.getEnvironment().getJDBCConnection()) {
			try (PreparedStatement ps = conn.prepareStatement(statement)) {
				setStatementArgs(ps, curve);
				ps.execute();
			}
			try (Statement stmt = conn.createStatement()) {
				stmt.execute("delete from phaedra.hca_curve_compound where curve_id = " + curve.getId());
				stmt.execute("delete from phaedra.hca_curve_property where curve_id = " + curve.getId());
			}
			insertCurveCompounds(curve, conn);
			insertCurveProperties(curve, conn);
			conn.commit();
		} catch (SQLException e) {
			throw new PersistenceException(e);
		}
	}
	
	private void setStatementArgs(PreparedStatement ps, Curve curve) throws SQLException {
		ps.setLong(1, curve.getFeature().getId());
		ps.setString(2, curve.getModelId());
		ps.setTimestamp(3, new Timestamp(curve.getFitDate().getTime()));
		ps.setString(4, curve.getFitVersion());
		ps.setBigDecimal(5, new BigDecimal(curve.getErrorCode()));
		String[] groupingValues = curve.getGroupingValues();
		if (groupingValues == null) groupingValues = new String[] {null,null,null};
		ps.setString(6, groupingValues.length <= 0 ? null : groupingValues[0]);
		ps.setString(7, groupingValues.length <= 1 ? null : groupingValues[1]);
		ps.setString(8, groupingValues.length <= 2 ? null : groupingValues[2]);
		if (curve.getPlot() == null) ps.setNull(9, Types.BINARY);
		else ps.setBinaryStream(9, new ByteArrayInputStream(curve.getPlot()), curve.getPlot().length);
	}
	
	private void insertCurveProperties(Curve curve, Connection conn) throws SQLException {
		Value[] properties = curve.getOutputParameters();
		if (properties == null || properties.length == 0) return;
		
		String[] arr = new String[CURVE_PROPERTY_COLUMNS.length];
		Arrays.fill(arr, "?");
		try (PreparedStatement ps = conn.prepareStatement("insert into phaedra.hca_curve_property"
					+ " (" + StringUtils.createSeparatedString(CURVE_PROPERTY_COLUMNS, ",") + " )"
					+ " values (" + StringUtils.createSeparatedString(arr, ",") + ")")) {
			
			for (int i = 0; i < properties.length; i++) {
				Value p = properties[i];
				
				ps.setLong(1, curve.getId());
				ps.setString(2, p.definition.name);
				
				if (p.definition.type.isNumeric()) ps.setDouble(3, p.numericValue);
				else ps.setNull(3, Types.DOUBLE);

				ps.setString(4, p.stringValue);

				if (p.binaryValue == null) ps.setNull(5, Types.BINARY);
				else ps.setBinaryStream(5, new ByteArrayInputStream(p.binaryValue), p.binaryValue.length);

				ps.addBatch();
			}
			
			ps.executeBatch();
		}
	}
	
	private void insertCurveCompounds(Curve curve, Connection conn) throws SQLException {
		try (PreparedStatement ps = conn.prepareStatement("insert into phaedra.hca_curve_compound (curve_id, platecompound_id) values (?, ?)")) {
			for (Compound compound: curve.getCompounds()) {
				ps.setLong(1, curve.getId());
				ps.setLong(2, compound.getId());
				ps.addBatch();
			}
			ps.executeBatch();
		}
	}
	
	private Curve toCurve(Object[] record) {
		long curveId = ((Number) record[0]).longValue();
		long featureId = ((Number) record[1]).longValue();
		Curve curve = new Curve();
		curve.setId(curveId);
		curve.setFeature(em.find(Feature.class, featureId));
		curve.setModelId((String) record[2]);
		curve.setFitDate((Date) record[3]);
		curve.setFitVersion((String) record[4]);
		
		curve.setErrorCode(((Number) record[5]).intValue());
		String[] groupingValues = new String[] {
				(String) record[6],
				(String) record[7],
				(String) record[8]
		};
		if (groupingValues[0] == null) groupingValues = null;
		curve.setGroupingValues(groupingValues);
		curve.setPlot((byte[]) record[9]);
		return curve;
	}
	
	private void applyProperties(Curve curve, List<?> resultSet) {
		ICurveFitModel model = CurveFitService.getInstance().getModel(curve.getModelId());
		List<Definition> outputParameterDefs = model.getOutputParameters(CurveFitService.getInstance().getSettings(curve));
		
		List<Value> properties = new ArrayList<>();
		for (Object record: resultSet) {
			Object[] row = (Object[]) record;
			
			long curveId = ((Number) row[0]).longValue();
			if (curveId != curve.getId()) continue;

			String name = (String) row[1];
			Definition def = CurveParameter.find(outputParameterDefs, name);
			if (def == null) continue;
			
			double numericValue = row[2] == null ? Double.NaN : (Double) row[2];
			String stringValue = (String) row[3];
			byte[] binaryValue = (byte[]) row[4];
			
			Value property = new Value(def, stringValue, numericValue, binaryValue);
			properties.add(property);
		}
		
		// Sort the properties as they are defined in the model.
		Value[] orderedValues = new Value[outputParameterDefs.size()];
		for (int i = 0; i < orderedValues.length; i++) {
			Definition def = outputParameterDefs.get(i);
			orderedValues[i] = properties.stream()
					.filter(v -> v.definition == def).findAny()
					.orElse(new Value(def, null, Double.NaN, null));
		}
		
		curve.setOutputParameters(orderedValues);
	}
}
