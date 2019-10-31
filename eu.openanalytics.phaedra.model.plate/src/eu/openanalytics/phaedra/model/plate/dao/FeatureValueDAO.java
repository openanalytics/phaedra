package eu.openanalytics.phaedra.model.plate.dao;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.persistence.PersistenceException;

import org.eclipse.core.runtime.Platform;
import org.postgresql.copy.CopyManager;
import org.postgresql.jdbc.PgConnection;

import eu.openanalytics.phaedra.base.db.IValueObject;
import eu.openanalytics.phaedra.base.db.JDBCUtils;
import eu.openanalytics.phaedra.base.environment.Screening;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.model.plate.Activator;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.FeatureValue;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;

public class FeatureValueDAO {

	private Lock updateLock;

	public FeatureValueDAO() {
		this.updateLock = new ReentrantLock();
	}

	public List<FeatureValue> getValues(List<Well> wells, Feature feature) {
		long startTime = System.currentTimeMillis();

		String queryString = "select fv.well_id, fv.feature_id,"
				+ " fv.raw_numeric_value, fv.raw_string_value, fv.normalized_value"
				+ " from phaedra.hca_feature_value fv"
				+ " where fv.feature_id = " + feature.getId()
				+ " and fv.well_id in (" + getCommaSeparatedIDs(wells) + ")";

		List<FeatureValue> values = null;
		try (Connection conn = getConnection()) {
			ResultSet resultSet = conn.createStatement().executeQuery(queryString);
			values = mapValues(resultSet, wells, Arrays.asList(feature));
		} catch (SQLException e) {
			throw new PersistenceException(e);
		}

		long duration = System.currentTimeMillis() - startTime;
		EclipseLog.debug("FeatureValue lookup in " + duration + "ms"
				+ " [Well " + wells.size()
				+ " Feature " + feature.getId() + "]", FeatureValueDAO.class);
		return values;
	}

	public List<FeatureValue> getValues(Well well, List<Feature> features) {
		long startTime = System.currentTimeMillis();

		String queryString = "select fv.well_id, fv.feature_id,"
				+ " fv.raw_numeric_value, fv.raw_string_value, fv.normalized_value"
				+ " from phaedra.hca_feature_value fv"
				+ " where fv.well_id = " + well.getId()
				+ " and fv.feature_id in (" + getCommaSeparatedIDs(features) + ")";

		List<FeatureValue> values = null;
		try (Connection conn = getConnection()) {
			ResultSet resultSet = conn.createStatement().executeQuery(queryString);
			values = mapValues(resultSet, Arrays.asList(well), features);
		} catch (SQLException e) {
			throw new PersistenceException(e);
		}

		long duration = System.currentTimeMillis() - startTime;
		EclipseLog.debug("FeatureValue lookup in " + duration + "ms"
				+ " [Well " + well.getId()
				+ " # of Features " + features.size() + "]", FeatureValueDAO.class);
		return values;
	}

	public List<FeatureValue> getValues(List<Well> wells, List<Feature> features) {
		long startTime = System.currentTimeMillis();

		String queryString = "select fv.well_id, fv.feature_id,"
				+ " fv.raw_numeric_value, fv.raw_string_value, fv.normalized_value"
				+ " from phaedra.hca_feature_value fv"
				+ " where fv.well_id in (" + getCommaSeparatedIDs(wells) + ")"
				+ " and fv.feature_id in (" + getCommaSeparatedIDs(features) + ")";

		List<FeatureValue> values = null;
		try (Connection conn = getConnection()) {
			ResultSet resultSet = conn.createStatement().executeQuery(queryString);
			values = mapValues(resultSet, wells, features);
		} catch (SQLException e) {
			throw new PersistenceException(e);
		}

		long duration = System.currentTimeMillis() - startTime;
		EclipseLog.debug("FeatureValue lookup in " + duration + "ms"
				+ " [# of wells " + wells.size()
				+ " # of Features " + features.size() + "]", FeatureValueDAO.class);
		return values;
	}

	/**
	 * Get the Feature Values for the given Plate and Features.
	 *
	 * <p>To get all Features for a Plate, use {@link #getValues(Plate plate)}
	 *
	 * @param plate
	 * @param features
	 * @return
	 */
	public List<FeatureValue> getValues(Plate plate, List<Feature> features) {
		long startTime = System.currentTimeMillis();

		String queryString = "select fv.well_id, fv.feature_id,"
				+ " fv.raw_numeric_value, fv.raw_string_value, fv.normalized_value"
				+ " from phaedra.hca_feature_value fv, phaedra.hca_plate_well w"
				+ " where w.plate_id = " + plate.getId()
				+ " and fv.well_id = w.well_id"
				+ " and fv.feature_id in (" + getCommaSeparatedIDs(features) + ")";

		List<FeatureValue> values = null;
		try (Connection conn = getConnection()) {
			ResultSet resultSet = conn.createStatement().executeQuery(queryString);
			values = mapValues(resultSet, plate);
		} catch (SQLException e) {
			throw new PersistenceException(e);
		}

		long duration = System.currentTimeMillis() - startTime;
		EclipseLog.debug("FeatureValue lookup in " + duration + "ms"
				+ " [Plate " + plate.getId()
				+ " # of Features " + features.size() + "]", FeatureValueDAO.class);
		return values;
	}

	public List<FeatureValue> getValues(Plate plate) {
		long startTime = System.currentTimeMillis();
		String queryString = "select fv.well_id, fv.feature_id,"
				+ " fv.raw_numeric_value, fv.raw_string_value, fv.normalized_value"
				+ " from phaedra.hca_feature_value fv, phaedra.hca_plate_well w"
				+ " where w.plate_id = " + plate.getId()
				+ " and fv.well_id = w.well_id";

		List<FeatureValue> values = null;
		try (Connection conn = getConnection()) {
			ResultSet resultSet = conn.createStatement().executeQuery(queryString);
			values = mapValues(resultSet, plate);
		} catch (SQLException e) {
			throw new PersistenceException(e);
		}

		long duration = System.currentTimeMillis() - startTime;
		EclipseLog.debug("FeatureValue lookup in " + duration + "ms"
				+ " [Plate " + plate.getId() + "]", FeatureValueDAO.class);
		return values;
	}

	/**
	 * Upsert (update or insert) the feature values for a specified Plate and Feature.
	 * Only one of rawValues, rawStringValues, normValues can be used at a time.
	 * Note that the length of the array must equal the number of wells.
	 *
	 * @param p The plate to update.
	 * @param f The feature to update.
	 * @param rawValues The new raw numeric values, ordered by well number.
	 * @param rawStringValues The new raw string values, ordered by well number.
	 * @param normValues The new normalized values, ordered by well number.
	 */
	public void updateValues(Plate p, Feature f, double[] rawValues, String[] rawStringValues, double[] normValues) {
		saveValues(p, f, -1, rawValues, rawStringValues, normValues);
	}
	
	/**
	 * Faster than {@link updateValues} but will throw
	 * a PK violation exception if values already exist for this plate and feature.
	 *
	 * @param p The plate to update.
	 * @param f The feature to update.
	 * @param rawValues The new raw numeric values, ordered by well number.
	 * @param rawStringValues The new raw string values, ordered by well number.
	 * @param normValues The new normalized values, ordered by well number.
	 */
	public void insertValues(Plate p, Feature f, double[] rawValues, String[] rawStringValues, double[] normValues) {
		saveValues(p, f, 0, rawValues, rawStringValues, normValues);
	}
	
	private void saveValues(Plate p, Feature f, int valueCount, double[] rawValues, String[] rawStringValues, double[] normValues) {
		long startTime = System.currentTimeMillis();
		int wellCount = p.getWells().size();

		String errorMsg = "Cannot save well data: the number of values must equal the number of wells";
		if (rawValues != null && rawValues.length != wellCount) throw new IllegalArgumentException(errorMsg);
		if (rawStringValues != null && rawStringValues.length != wellCount) throw new IllegalArgumentException(errorMsg);
		if (normValues != null && normValues.length != wellCount) throw new IllegalArgumentException(errorMsg);

		PreparedStatement ps = null;
		updateLock.lock();
		try (Connection conn = getConnection()) {
			if (valueCount == -1) {
				String queryString = "select count(*) from phaedra.hca_feature_value fv"
						+ " where fv.feature_id = " + f.getId()
						+ " and fv.well_id in (select well_id 		from phaedra.hca_plate_well where plate_id = " + p.getId() + ")"
						+ " and fv.well_id >= (select min(well_id)	from phaedra.hca_plate_well where plate_id = " + p.getId() + ")"
						+ " and fv.well_id <= (select max(well_id)	from phaedra.hca_plate_well where plate_id = " + p.getId() + ")";
				valueCount = getCount(queryString, conn);
			}
			
			if (valueCount > 0 && valueCount != wellCount) {
				// Some values exist. Delete, then insert.
				String queryString = "delete from phaedra.hca_feature_value where well_id = ? and feature_id = ?";
				ps = conn.prepareStatement(queryString);

				for (Well well: p.getWells()) {
					ps.setLong(1, well.getId());
					ps.setLong(2, f.getId());
					ps.addBatch();
				}
				ps.executeBatch();
				conn.commit();
				ps.close();
			}

			if (valueCount == wellCount) {
				// Update all values
				String fieldName = "raw_numeric_value";
				if (rawStringValues != null) fieldName = "raw_string_value";
				if (normValues != null) fieldName = "normalized_value";

				String queryString = "update phaedra.hca_feature_value set " + fieldName + " = ?" + " where well_id = ? and feature_id = ?";
				ps = conn.prepareStatement(queryString);

				for (Well well: p.getWells()) {
					int wellNr = PlateUtils.getWellNr(well);
					if (rawValues != null) ps.setDouble(1, rawValues[wellNr-1]);
					else if (rawStringValues != null) ps.setString(1, rawStringValues[wellNr-1]);
					else if (normValues != null) ps.setDouble(1, normValues[wellNr-1]);
					ps.setLong(2, well.getId());
					ps.setLong(3, f.getId());
					ps.addBatch();
				}
				ps.executeBatch();
				conn.commit();
			} else {
				// Insert all values
				if (JDBCUtils.isPostgres()) {
					insertValuesPostgres(p, f, rawValues, rawStringValues, normValues);
				} else {
					String queryString = "insert into phaedra.hca_feature_value (raw_numeric_value, raw_string_value, normalized_value, well_id, feature_id) values(?,?,?,?,?)";
					ps = conn.prepareStatement(queryString);
					
					for (Well well: p.getWells()) {
						int wellNr = PlateUtils.getWellNr(well);
						double raw = (rawValues == null) ? Double.NaN : rawValues[wellNr-1];
						String str = (rawStringValues == null) ? null : rawStringValues[wellNr-1];
						double norm = (normValues == null) ? Double.NaN : normValues[wellNr-1];
						ps.setDouble(1, raw);
						ps.setString(2, str);
						ps.setDouble(3, norm);
						ps.setLong(4, well.getId());
						ps.setLong(5, f.getId());
						ps.addBatch();
					}
					ps.executeBatch();
					conn.commit();
				}
			}
		} catch (SQLException e) {
			throw new PersistenceException(e);
		} finally {
			updateLock.unlock();
			if (ps != null) try { ps.close(); } catch (SQLException e) {}
		}

		long duration = System.currentTimeMillis() - startTime;
		EclipseLog.debug("FeatureValue update in " + duration + "ms" + " [" + p.getWells().size() + " values]", FeatureValueDAO.class);
	}

	/*
	 * **********
	 * Non-public
	 * **********
	 */

	private int getCount(String countQuery, Connection conn) throws SQLException {
		try (ResultSet resultSet = conn.createStatement().executeQuery(countQuery)) {
			resultSet.next();
			return resultSet.getInt(1);
		}
	}

	private List<FeatureValue> mapValues(ResultSet set, Plate plate) {
		return mapValues(set, plate.getWells(), PlateUtils.getFeatures(plate));
	}

	private List<FeatureValue> mapValues(ResultSet set, List<Well> wells, List<Feature> features) {
		List<FeatureValue> values = new ArrayList<>();

		try {
			if (!set.isBeforeFirst()) return values;

			while (set.next()) {
				long wellId = set.getLong(1);
				long featureId = set.getLong(2);
				Double rawNum = (Double) set.getObject(3);
				String rawStr = set.getString(4);
				Double normNum = (Double) set.getObject(5);

				FeatureValue v = new FeatureValue();
				v.setWellId(wellId);
				for (Well well: wells) {
					if (well.getId() == wellId) {
						v.setWell(well);
						break;
					}
				}
				v.setFeatureId(featureId);
				for (Feature f: features) {
					if (f.getId() == featureId) {
						v.setFeature(f);
						break;
					}
				}
				// Ignore unknown features (e.g. just created by another Phaedra client)
				if (v.getFeature() == null) continue;

				if (rawNum != null) v.setRawNumericValue(rawNum);
				v.setRawStringValue(rawStr);
				if (normNum != null) v.setNormalizedValue(normNum);
				values.add(v);
			}
		} catch (SQLException e) {
			EclipseLog.error(e.getMessage(), e, Activator.getDefault());
		}

		return values;
	}

	private <E extends IValueObject> String getCommaSeparatedIDs(List<E> list) {
		// EclipseLink Java 8 stream bug bypass.
		List<E> newList = new ArrayList<>(list);
		return newList.stream().map(o -> o.getId()+"").collect(Collectors.joining(","));
	}

	private void insertValuesPostgres(Plate p, Feature f, double[] rawValues, String[] rawStringValues, double[] normValues) {
		Consumer<OutputStream> csvStreamer = out -> {
			try (BufferedWriter csvWriter = new BufferedWriter(new OutputStreamWriter(out))) {
				for (Well well: p.getWells()) {
					int wellNr = PlateUtils.getWellNr(well);
					double raw = (rawValues == null) ? Double.NaN : rawValues[wellNr-1];
					String str = (rawStringValues == null) ? null : rawStringValues[wellNr-1];
					double norm = (normValues == null) ? Double.NaN : normValues[wellNr-1];
					String[] line = {
							String.valueOf(well.getId()),
							String.valueOf(f.getId()),
							String.valueOf(raw),
							String.valueOf(str),
							String.valueOf(norm)
					};
					csvWriter.write(Arrays.stream(line).collect(Collectors.joining(",")) + "\n");
				}
				csvWriter.flush();
			} catch (Exception e) {
				EclipseLog.error(String.format("Failed to stream welldata to CSV"), e, Platform.getBundle(Activator.class.getPackage().getName()));
			}
		};
		
		try (Connection conn = getConnection()) {
			String sql = String.format("copy phaedra.hca_feature_value (well_id,feature_id,raw_numeric_value,raw_string_value,normalized_value) from stdin (format csv)");
			CopyManager cm = conn.unwrap(PgConnection.class).getCopyAPI();
			
			try (PipedInputStream input = new PipedInputStream(1024*1024*10)) {
				PipedOutputStream output = new PipedOutputStream(input);
				new Thread(() -> { csvStreamer.accept(output); } , "Data Stream Copier").start();
				cm.copyIn(sql, input);
			}
			conn.commit();
		} catch (IOException | SQLException e) {
			throw new PersistenceException(e);
		}
	}
	
	private Connection getConnection() {
		return Screening.getEnvironment().getJDBCConnection();
	}
}
