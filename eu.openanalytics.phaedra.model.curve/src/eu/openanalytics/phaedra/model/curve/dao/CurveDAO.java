package eu.openanalytics.phaedra.model.curve.dao;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
import eu.openanalytics.phaedra.model.curve.CurveProperty;
import eu.openanalytics.phaedra.model.curve.util.CurveFactory;
import eu.openanalytics.phaedra.model.curve.util.CurveGrouping;
import eu.openanalytics.phaedra.model.curve.vo.Curve;
import eu.openanalytics.phaedra.model.curve.vo.CurveSettings;
import eu.openanalytics.phaedra.model.plate.vo.Compound;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;

public class CurveDAO {

	private static final String[] CURVE_COLUMNS = {
		"CURVE_ID","FEATURE_ID",
		"CURVE_KIND","CURVE_METHOD","CURVE_MODEL","CURVE_TYPE",
		"FIT_DATE","FIT_VERSION","FIT_ERROR",
		"EMAX","EMAX_CONC","PLOT"
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
				+ " from phaedra.hca_curve where curve_id = " + curveId;
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
				+ " where c.feature_id = " + f.getId() + " and c.curve_id = cc.curve_id and cc.platecompound_id = " + c.getId();
		for (int i = 0; i < grouping.getCount(); i++) {
			queryString += " and exists (select cp.string_value from phaedra.hca_curve_property cp where cp.curve_id = c.curve_id"
					+ " and cp.property_name = 'GROUP_BY_" + (i+1) + "'"	
					+ " and cp.string_value = '" + grouping.get(i) + "')";
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
	
	public void deleteIncompatibleCurves(Curve curve) {
		String compIds = StringUtils.createSeparatedString(curve.getCompounds(), c -> "" + c.getId(), ",");
		String statement = "delete from phaedra.hca_curve c"
				+ " where c.feature_id = " + curve.getFeature().getId()
				+ " and c.curve_id != " + curve.getId()
				+ " and c.curve_id in (select cc.curve_id from phaedra.hca_curve_compound cc where cc.platecompound_id in (" + compIds + "))";
		if (curve.getSettings().getGroupBy1() != null) {
			// If the curve has grouping, delete any ungrouped curves on the same compound(s).
			statement += " and not exists (select cp.* from phaedra.hca_curve_property cp where c.curve_id = cp.curve_id and cp.property_name = 'GROUP_BY_1' and cp.string_value is not null)";
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
	
	/*
	 * Non-public
	 * **********
	 */
	
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
		
		PreparedStatement ps = null;
		try (Connection conn = Screening.getEnvironment().getJDBCConnection()) {
			ps = conn.prepareStatement(statement);
			setStatementArgs(ps, curve);
			ps.execute();
			conn.commit();
		} catch (SQLException e) {
			throw new PersistenceException(e);
		} finally {
			if (ps != null) try { ps.close(); } catch (SQLException e) {}
		}
		
		insertCurveCompounds(curve);
		insertCurveProperties(curve);
	}
	
	private void updateCurve(Curve curve) {
		String[] updateColumns = new String[CURVE_COLUMNS.length - 1];
		System.arraycopy(CURVE_COLUMNS, 1, updateColumns, 0, updateColumns.length);
		String updateColumnsString = StringUtils.createSeparatedString(updateColumns, "=?,");
		String statement = "update phaedra.hca_curve set " + updateColumnsString + "=? where curve_id = " + curve.getId();
		
		PreparedStatement ps = null;
		try (Connection conn = Screening.getEnvironment().getJDBCConnection()) {
			ps = conn.prepareStatement(statement);
			setStatementArgs(ps, curve);
			ps.execute();
			conn.commit();
		} catch (SQLException e) {
			throw new PersistenceException(e);
		} finally {
			if (ps != null) try { ps.close(); } catch (SQLException e) {}
		}
		
		Statement stmt = null;
		try (Connection conn = Screening.getEnvironment().getJDBCConnection()) {
			stmt = conn.createStatement();
			stmt.execute("delete from phaedra.hca_curve_property where curve_id = " + curve.getId());
			stmt.execute("delete from phaedra.hca_curve_compound where curve_id = " + curve.getId());
			conn.commit();
		} catch (SQLException e) {
			throw new PersistenceException(e);
		} finally {
			if (stmt != null) try { stmt.close(); } catch (SQLException e) {}
		}
		insertCurveProperties(curve);
		insertCurveCompounds(curve);
	}
	
	private void setStatementArgs(PreparedStatement ps, Curve curve) throws SQLException {
		ps.setLong(1, curve.getFeature().getId());
		ps.setString(2, curve.getSettings().getKind());
		ps.setString(3, curve.getSettings().getMethod());
		ps.setString(4, curve.getSettings().getModel());
		ps.setString(5, curve.getSettings().getType());
		
		ps.setTimestamp(6, new Timestamp(curve.getFitDate().getTime()));
		ps.setString(7, curve.getFitVersion());
		ps.setBigDecimal(8, new BigDecimal(curve.getFitError()));
		ps.setDouble(9, curve.geteMax());
		ps.setBigDecimal(10, new BigDecimal(curve.geteMaxConc()));
		
		if (curve.getPlot() == null) ps.setNull(11, Types.BINARY);
		else ps.setBinaryStream(11, new ByteArrayInputStream(curve.getPlot()), curve.getPlot().length);
	}
	
	private void insertCurveProperties(Curve curve) {
		PreparedStatement ps = null;
		try (Connection conn = Screening.getEnvironment().getJDBCConnection()) {
			String[] arr = new String[CURVE_PROPERTY_COLUMNS.length];
			Arrays.fill(arr, "?");
			ps = conn.prepareStatement("insert into phaedra.hca_curve_property"
					+ " (" + StringUtils.createSeparatedString(CURVE_PROPERTY_COLUMNS, ",") + " )"
					+ " values (" + StringUtils.createSeparatedString(arr, ",") + ")");
			
			for (CurveProperty p: CurveProperty.values()) {
				if (!p.sameKind(curve) && p != CurveProperty.GROUP_BY_1 && p != CurveProperty.GROUP_BY_2 && p != CurveProperty.GROUP_BY_3) continue;
				ps.setLong(1, curve.getId());
				ps.setString(2, p.name());
				
				Object value = p.getValue(curve);
				Double numVal = (value instanceof Double) ? (Double) value : null;
				String strVal = (value instanceof String) ? (String) value : null;
				byte[] binVal = (value instanceof byte[]) ? (byte[]) value : null;
				if (value instanceof Integer) numVal = ((Integer) value).doubleValue();
				if (numVal == null && strVal == null && binVal == null && value != null) binVal = serialize(value);

				if (numVal == null) ps.setNull(3, Types.DOUBLE);
				else ps.setDouble(3, numVal);
				ps.setString(4, strVal);
				if (binVal == null) ps.setNull(5, Types.BINARY);
				else ps.setBinaryStream(5, new ByteArrayInputStream(binVal), binVal.length);
				
				ps.addBatch();
			}
			
			ps.executeBatch();
			conn.commit();
		} catch (SQLException e) {
			e.getNextException().printStackTrace();
			throw new PersistenceException(e);
		} finally {
			if (ps != null) try { ps.close(); } catch (SQLException e) {}
		}
	}
	
	private void insertCurveCompounds(Curve curve) {
		PreparedStatement ps = null;
		try (Connection conn = Screening.getEnvironment().getJDBCConnection()) {
			ps = conn.prepareStatement("insert into phaedra.hca_curve_compound (curve_id, platecompound_id) values (?, ?)");
			for (Compound compound: curve.getCompounds()) {
				ps.setLong(1, curve.getId());
				ps.setLong(2, compound.getId());
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
	
	private Curve toCurve(Object[] record) {
		long curveId = ((Number) record[0]).longValue();
		long featureId = ((Number) record[1]).longValue();
		
		Curve curve = CurveFactory.newCurve((String) record[2]);
		curve.setId(curveId);
		curve.setFeature(em.find(Feature.class, featureId));
		
		CurveSettings settings = new CurveSettings();
		curve.setSettings(settings);
		settings.setKind((String) record[2]);
		settings.setMethod((String) record[3]);
		settings.setModel((String) record[4]);
		settings.setType((String) record[5]);
		
		curve.setFitDate((Date) record[6]);
		curve.setFitVersion((String) record[7]);
		curve.setFitError(((Number) record[8]).intValue());
		
		curve.seteMax((Double) record[9]);
		if (record[10] == null) curve.seteMaxConc(Double.NaN);
		else curve.seteMaxConc(((Number) record[10]).doubleValue());
		curve.setPlot((byte[]) record[11]);

		return curve;
	}
	
	private void applyProperties(Curve curve, List<?> resultSet) {
		for (Object record: resultSet) {
			Object[] row = (Object[]) record;
			
			long curveId = ((Number) row[0]).longValue();
			if (curveId != curve.getId()) continue;
			CurveProperty p = CurveProperty.getByName((String) row[1]);
			if (!p.sameKind(curve) && p != CurveProperty.GROUP_BY_1 && p != CurveProperty.GROUP_BY_2 && p != CurveProperty.GROUP_BY_3) continue;
			
			Object value = row[2];
			if (value == null) value = row[3];
			if (value == null) value = row[4];
			if (value == null) continue;
			
			if (p == CurveProperty.CI_GRID || p == CurveProperty.WEIGHTS) value = deserialize((byte[]) value);
			if (p == CurveProperty.FIT_ERROR || p == CurveProperty.NIC || p == CurveProperty.NAC) value = ((Double) value).intValue();
			if (value instanceof Number) value = ((Number) value).doubleValue();
			
			p.setValue(curve, value);
		}
	}
	
	private byte[] serialize(Object data) {
		if (data == null) return null;
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(bos);
			oos.writeObject(data);
			return bos.toByteArray();
		} catch (IOException e) {
			return null;
		}
	}

	private Object deserialize(byte[] data) {
		if (data == null) return null;
		try {
			ByteArrayInputStream bis = new ByteArrayInputStream(data);
			ObjectInputStream iis = new ObjectInputStream(bis);
			return iis.readObject();
		} catch (Exception e) {
			return null;
		}
	}
}
