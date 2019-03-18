package eu.openanalytics.phaedra.model.plate;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;

import eu.openanalytics.phaedra.base.cache.CacheService;
import eu.openanalytics.phaedra.base.cache.ICache;
import eu.openanalytics.phaedra.base.db.JDBCUtils;
import eu.openanalytics.phaedra.base.db.jpa.BaseJPAService;
import eu.openanalytics.phaedra.base.environment.Screening;
import eu.openanalytics.phaedra.base.event.ModelEvent;
import eu.openanalytics.phaedra.base.event.ModelEventService;
import eu.openanalytics.phaedra.base.event.ModelEventType;
import eu.openanalytics.phaedra.base.imaging.jp2k.CodecFactory;
import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.base.security.model.Permissions;
import eu.openanalytics.phaedra.base.util.io.FileUtils;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.model.plate.dao.FeatureValueDAO;
import eu.openanalytics.phaedra.model.plate.dao.PlatePropertyDAO;
import eu.openanalytics.phaedra.model.plate.hook.PlateActionHookManager;
import eu.openanalytics.phaedra.model.plate.hook.WellDataActionHookManager;
import eu.openanalytics.phaedra.model.plate.util.ExperimentSummary;
import eu.openanalytics.phaedra.model.plate.util.ObjectCopyFactory;
import eu.openanalytics.phaedra.model.plate.util.PlateSummary;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Compound;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;
import eu.openanalytics.phaedra.model.plate.vo.FeatureValue;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.Activator;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.model.protocol.vo.Protocol;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;

/**
 * API for interaction with plates. This includes creation, retrieval and modification of plates
 * and their related objects:
 * <ul>
 * <li>Experiments</li>
 * <li>Wells</li>
 * <li>Compounds</li>
 * <li>Well data (i.e. well feature values)</li>
 * </ul>
 */
public class PlateService extends BaseJPAService {

	private static PlateService instance = new PlateService();

	private FeatureValueDAO featureValueDAO;
	private PlatePropertyDAO platePropertyDAO;

	private PlateService() {
		// Hidden constructor
		featureValueDAO = new FeatureValueDAO(getEntityManager());
		platePropertyDAO = new PlatePropertyDAO(getEntityManager());
	}

	public static PlateService getInstance() {
		return instance;
	}

	@Override
	protected EntityManager getEntityManager() {
		return Screening.getEnvironment().getEntityManager();
	}

	/**
	 * Get a list of all experiments in a protocol.
	 * 
	 * @param protocol The protocol whose experiments will be listed.
	 * @return A list of experiments currently in the protocol.
	 */
	public List<Experiment> getExperiments(Protocol protocol) {
		if (!SecurityService.getInstance().check(Permissions.PROTOCOL_OPEN, protocol)) return new ArrayList<>();
		String query = "select e from Experiment e where e.protocol = ?1";
		return streamableList(getList(query, Experiment.class, protocol));
	}

	/**
	 * Retrieve an experiment by its primary ID.
	 * 
	 * @param experimentId The ID of the experiment.
	 * @return The experiment, or null if no matching experiment was found.
	 */
	public Experiment getExperiment(long experimentId) {
		Experiment exp = getEntity("select e from Experiment e where e.id = ?1", Experiment.class, experimentId);
		if (!SecurityService.getInstance().check(Permissions.EXPERIMENT_OPEN, exp)) return null;
		return exp;
	}

	/**
	 * Delete an experiment.
	 * 
	 * @param experiment The experiment to delete.
	 */
	public void deleteExperiment(Experiment experiment) {
		SecurityService.getInstance().checkWithException(Permissions.EXPERIMENT_DELETE, experiment);
		List<Plate> plates = getPlates(experiment);
		deletePlates(plates);
		delete(experiment);
	}

	/**
	 * Update an experiment. Any changes made to the experiment object will be saved.
	 * 
	 * @param experiment The experiment to update.
	 */
	public void updateExperiment(Experiment experiment) {
		SecurityService.getInstance().checkWithException(Permissions.EXPERIMENT_EDIT, experiment);
		if (experiment.getName() == null) throw new IllegalArgumentException("Experiment name cannot be null.");
		save(experiment);
	}

	/**
	 * Create a new experiment. Make sure to call {@link PlateService#updateExperiment(Experiment)} afterwards.
	 * 
	 * @param protocol The parent protocol of the experiment.
	 * @return The new experiment, not yet saved.
	 */
	public Experiment createExperiment(Protocol protocol) {
		SecurityService.getInstance().checkWithException(Permissions.EXPERIMENT_CREATE, protocol);
		Experiment experiment = new Experiment();
		String currentUser = SecurityService.getInstance().getCurrentUserName();
		experiment.setCreator(currentUser);
		experiment.setCreateDate(new Date());
		experiment.setName("New Experiment");
		experiment.setProtocol(protocol);
		return experiment;
	}

	/**
	 * Move a list of experiments to a different protocol.
	 * <p>
	 * <b>Important</b>: this should not be used to move an experiment to a protocol of a different protocol class.
	 * Doing so may corrupt the experiment and all its data.
	 * </p>
	 * 
	 * @param experiments The experiments to move.
	 * @param newProtocol The new parent protocol.
	 */
	public void moveExperiments(List<Experiment> experiments, Protocol newProtocol) {
		SecurityService.getInstance().checkWithException(Permissions.EXPERIMENT_CREATE, newProtocol);
		for (Experiment experiment: experiments) {
			SecurityService.getInstance().checkWithException(Permissions.EXPERIMENT_MOVE, experiment);
			experiment.setProtocol(newProtocol);
		}
		saveCollection(experiments);
	}

	/**
	 * Get a "to-do summary" of an experiment.
	 * 
	 * @param experiment The experiment to get a summary for.
	 * @return A summary for the given experiment.
	 */
	public ExperimentSummary getExperimentSummary(Experiment experiment) {
		ExperimentSummary summary = new ExperimentSummary();

		String sql = "select"
				+ " (select count(plate_id) from phaedra.hca_plate where experiment_id = ?) plates,"
				+ " (select count(plate_id) from phaedra.hca_plate where experiment_id = ? and calc_status = 0) platesToCalc,"
				+ " (select count(plate_id) from phaedra.hca_plate where experiment_id = ? and validate_status = 0) platesToValidate,"
				+ " (select count(plate_id) from phaedra.hca_plate where experiment_id = ? and approve_status = 0) platesToApprove,"
				+ " (select count(plate_id) from phaedra.hca_plate where experiment_id = ? and upload_status = 0 and approve_status >= 0) platesToExport,"
				+ " (select count(pc.platecompound_id) from phaedra.hca_plate_compound pc, phaedra.hca_plate p"
				+ "		where pc.plate_id = p.plate_id and p.experiment_id = ?"
				+ "		and (select count(w.well_id) from phaedra.hca_plate_well w where w.platecompound_id = pc.platecompound_id) > 3) crc_count,"
				+ " (select count(pc.platecompound_id) from phaedra.hca_plate_compound pc, phaedra.hca_plate p"
				+ "		where pc.plate_id = p.plate_id and p.experiment_id = ?"
				+ "		and (select count(w.well_id) from phaedra.hca_plate_well w where w.platecompound_id = pc.platecompound_id) <= 3) screen_count"
				+ JDBCUtils.getFromDual();
		sql = sql.replace("?", "" + experiment.getId());

		PreparedStatement ps = null;
		try (Connection conn = Screening.getEnvironment().getJDBCConnection()) {
			ps = conn.prepareStatement(sql);
			ResultSet resultSet = ps.executeQuery();

			if (resultSet.next()) {
				summary.plates = resultSet.getInt(1);
				summary.platesToCalculate = resultSet.getInt(2);
				summary.platesToValidate = resultSet.getInt(3);
				summary.platesToApprove = resultSet.getInt(4);
				summary.platesToExport = resultSet.getInt(5);
				summary.crcCount = resultSet.getInt(6);
				summary.screenCount = resultSet.getInt(7);
			}
		} catch (SQLException e) {
			EclipseLog.error(e.getMessage(), e, Activator.getDefault());
		} finally {
			if (ps != null) try { ps.close(); } catch (SQLException e) {}
		}

		return summary;
	}

	/**
	 * Get a list of plates in a given experiment.
	 * 
	 * @param experiment The parent experiment.
	 * @return A list of plates in the experiment.
	 */
	public List<Plate> getPlates(Experiment experiment) {
		if (!SecurityService.getInstance().check(Permissions.EXPERIMENT_OPEN, experiment)) return new ArrayList<>();
		return streamableList(getList("select p from Plate p where p.experiment = ?1", Plate.class, experiment));
	}

	/**
	 * Get a list of all plates in a protocol class that match the given barcode pattern.
	 * 
	 * @param barcodePattern The barcode, or part of it.
	 * @param pClass The parent protocol class.
	 * @return A list of matching plates.
	 */
	public List<Plate> getPlates(String barcodePattern, ProtocolClass pClass) {
		String query = "select p from Plate p where p.barcode like ?1 and p.experiment.protocol.protocolClass = ?2";
		barcodePattern = "%" + barcodePattern + "%";
		return streamableList(getList(query, Plate.class, barcodePattern, pClass)).stream()
				.filter(p -> SecurityService.getInstance().check(Permissions.PLATE_OPEN, p))
				.collect(Collectors.toList());
	}

	/**
	 * Retrieve a plate by its primary ID.
	 * 
	 * @param plateId The ID of the plate.
	 * @return The plate, or null if no matching plate was found.
	 */
	public Plate getPlateById(long plateId) {
		Plate plate = getEntity(Plate.class, plateId);
		if (!SecurityService.getInstance().check(Permissions.PLATE_OPEN, plate)) return null;
		return plate;
	}
	
	/**
	 * Create a new plate. Make sure to call {@link PlateService#updatePlate(Plate)} afterwards.
	 * 
	 * @param exp The parent experiment to create the plate in.
	 * @param rows The number of rows in the plate.
	 * @param cols The number of columns in the plate.
	 * @return A new plate, not yet saved.
	 */
	public Plate createPlate(Experiment exp, int rows, int cols) {
		SecurityService.getInstance().checkWithException(Permissions.PLATE_CREATE, exp);
		
		Plate plate =  new Plate();
		plate.setExperiment(exp);
		plate.setRows(rows);
		plate.setColumns(cols);

		String imagePath = exp.getProtocol().getProtocolClass().getId() + "/" + FileUtils.createYearWeekString();
		plate.setImagePath(imagePath);

		List<Well> wells = new ArrayList<Well>();
		for (int row=0; row<rows; row++) {
			for (int col=0; col<cols; col++) {
				Well well = new Well();
				well.setRow(row+1);
				well.setColumn(col+1);
				well.setPlate(plate);
				well.setWellType("EMPTY");
				wells.add(well);
			}
		}
		plate.setWells(wells);
		return plate;
	}

	/**
	 * Save the changes made to a plate or its wells.
	 * 
	 * Depending on the plate's validation and approval status, this operation may be prohibited.
	 * To modify a plate's validation or approval status, use {@link PlateService#updatePlateValidation(Plate)} instead.
	 * To modify the well data of a plate, use {@link PlateService#updateWellDataRaw(Plate, Feature, double[])} or one
	 * of its sibling methods instead.
	 * 
	 * @param plate The plate to update.
	 */
	public void updatePlate(Plate plate) {
		updatePlate(plate, ModelEventType.ObjectChanged);
	}

	/**
	 * Save the changes made to a plate's validation or approval state.
	 * 
	 * @param plate The plate to update.
	 */
	public void updatePlateValidation(Plate plate) {
		updatePlate(plate, ModelEventType.ValidationChanged);
	}

	private void updatePlate(Plate plate, ModelEventType changeType) {
		SecurityService.getInstance().checkWithException(Permissions.PLATE_EDIT, plate);
		try {
			PlateActionHookManager.preAction(plate, changeType);
			save(plate);
			PlateActionHookManager.postAction(plate, changeType);
		} catch (Throwable t) {
			// Undo the local changes to the plate.
			try { getEntityManager().refresh(plate); } catch (Exception e) {}
			throw t;
		}
	}

	/**
	 * Move a list of plates to a different experiment.
	 * <p>
	 * <b>Important</b>: this should not be used to move a plate to an experiment of a different protocol class.
	 * Doing so may corrupt the plate and all its data.
	 * </p>
	 * @param plates The plates to move.
	 * @param newExperiment The new parent experiment.
	 */
	public void movePlates(List<Plate> plates, Experiment newExperiment) {
		SecurityService.getInstance().checkWithException(Permissions.PLATE_CREATE, newExperiment);
		PlateActionHookManager.startBatch();
		try {
			for (Plate plate: plates) {
				SecurityService.getInstance().checkWithException(Permissions.PLATE_MOVE, plate);
				PlateActionHookManager.preAction(plate, ModelEventType.ObjectMoved);
				plate.setExperiment(newExperiment);
				save(plate);
				PlateActionHookManager.postAction(plate, ModelEventType.ObjectMoved);
			}
		} catch (Throwable t) {
			PlateActionHookManager.endBatch(false);
			throw t;
		}
		PlateActionHookManager.endBatch(true);
	}

	/**
	 * Create a full copy of a plate in the same experiment.
	 * 
	 * This includes the plate's properties, its well data, image data and subwell data.
	 * The following properties are NOT copied:
	 * <ul>
	 * <li>Validation and approval status</li>
	 * <li>Curve fits and manual curve settings</li>
	 * <li>Validation history</li>
	 * </ul>
	 * The plate will be recalculated immediately after the copy.
	 * 
	 * @param plate The plate to clone.
	 * @param monitor A progress monitor to update the clone progress (optional).
	 * @return The newly created copy.
	 */
	public Plate clonePlate(Plate plate, IProgressMonitor monitor) {
		SecurityService.getInstance().checkWithException(Permissions.PLATE_CREATE, plate.getExperiment());
		
		if (monitor == null) monitor = new NullProgressMonitor();
		monitor.beginTask("Cloning " + plate, 100);
		
		monitor.subTask("Cloning objects");
		Plate copy = createPlate(plate.getExperiment(), plate.getRows(), plate.getColumns());
		save(copy);
		String imagePath = copy.getImagePath();
		ObjectCopyFactory.copySettings(plate, copy, false, false);
		copy.setImagePath(imagePath);
		saveCollection(copy.getCompounds());
		copy.setDescription("Copy of " + plate);
		save(copy);
		setPlateProperties(copy, getPlateProperties(plate));
		monitor.worked(10);
		if (monitor.isCanceled()) {
			deletePlate(copy);
			return null;
		}
		
		monitor.subTask("Copying well data");
		List<FeatureValue> wellData = getWellData(plate);
		List<FeatureValue> newData = new ArrayList<>();
		for (FeatureValue v: wellData) {
			FeatureValue newV = new FeatureValue();
			newV.setFeature(v.getFeature());
			newV.setWell(PlateUtils.getWell(copy, v.getWell().getRow(), v.getWell().getColumn()));
			newV.setRawNumericValue(v.getRawNumericValue());
			newV.setNormalizedValue(v.getNormalizedValue());
			newV.setRawStringValue(v.getRawStringValue());
			newData.add(newV);
		}
		monitor.worked(10);
		
		for (Feature f: PlateUtils.getFeatures(plate)) {
			if (monitor.isCanceled()) {
				deletePlate(copy);
				return null;
			}
			
			List<FeatureValue> featureData = newData.stream().filter(v -> v.getFeature() == f).collect(Collectors.toList());
			if (f.isNumeric()) {
				double[] rawData = new double[PlateUtils.getWellCount(copy)];
				featureData.stream().forEach(v -> rawData[PlateUtils.getWellNr(v.getWell())-1] = v.getRawNumericValue());
				featureValueDAO.insertValues(copy, f, rawData, null, null);
				// Note: normalized values are saved later (during recalculation)
			} else {
				String[] rawData = new String[PlateUtils.getWellCount(copy)];
				featureData.stream().forEach(v -> rawData[PlateUtils.getWellNr(v.getWell())-1] = v.getRawStringValue());
				featureValueDAO.insertValues(copy, f, null, rawData, null);
			}
		}
		monitor.worked(20);
		
		monitor.subTask("Copying images");
		if (plate.isImageAvailable()) {
			try {
				String fromImagePath = getImageFSPath(plate);
				String toImagePath = getPlateFSPath(copy) + "/" + copy.getId() + "." + FileUtils.getExtension(fromImagePath);
				Screening.getEnvironment().getFileServer().copy(fromImagePath, toImagePath);
			} catch (IOException e) {
				deletePlate(copy);
				throw new RuntimeException("Failed to clone plate: error while copying image", e);
			}
		}
		monitor.worked(30);
		if (monitor.isCanceled()) {
			deletePlate(copy);
			return null;
		}

		monitor.subTask("Copying subwell data");
		if (plate.isSubWellDataAvailable()) {
			try {
				String fromHDF5Path = getPlateFSPath(plate) + "/" + plate.getId() + ".h5";
				String toHDF5Path = getPlateFSPath(copy) + "/" + copy.getId() + ".h5";
				Screening.getEnvironment().getFileServer().copy(fromHDF5Path, toHDF5Path);
			} catch (IOException e) {
				deletePlate(copy);
				throw new RuntimeException("Failed to clone plate: error while copying subwell data", e);
			}
		}
		monitor.worked(30);
		if (monitor.isCanceled()) {
			deletePlate(copy);
			return null;
		}
		
		PlateActionHookManager.postAction(copy, ModelEventType.ObjectCloned);
		
		monitor.done();
		return copy;
	}
	
	/**
	 * Delete a list of plates.
	 * 
	 * @param plates The plates to delete.
	 */
	public void deletePlates(List<Plate> plates) {
		PlateActionHookManager.startBatch();
		try {
			for (Plate plate: plates) {
				deletePlate(plate);
			}
		} catch (Throwable t) {
			PlateActionHookManager.endBatch(false);
			throw t;
		}
		PlateActionHookManager.endBatch(true);
	}

	/**
	 * Delete a single plate.
	 * 
	 * @param plate The plate to delete.
	 */
	public void deletePlate(Plate plate) {
		SecurityService.getInstance().checkWithException(Permissions.PLATE_DELETE, plate);

		PlateActionHookManager.preAction(plate, ModelEventType.ObjectRemoved);

		// Delete the plate object (cascades to wells, compounds, feature values and curve results)
		delete(plate);
		
		// Delete the plate files (image and subwell data)
		String plateFSPath = null;
		try {
			plateFSPath = getPlateFSPath(plate);
			Screening.getEnvironment().getFileServer().delete(plateFSPath + "/");
		} catch (IOException e) {
			Activator.getDefault().getLog().log(new Status(IStatus.ERROR, Activator.PLUGIN_ID,
					"Failed to clean up plate files at " + plateFSPath, e));
		}

		PlateActionHookManager.postAction(plate, ModelEventType.ObjectRemoved);
	}

	/**
	 * Get a summary of a plate, including the number of single-dose compounds and dose-response curves.
	 * 
	 * @param plate The plate to get a summary for.
	 * @return A summary for the given plate.
	 */
	public PlateSummary getPlateSummary(Plate plate) {
		PlateSummary summary = new PlateSummary();

		String sql = "select"
				+ " (select count(pc.platecompound_id) from phaedra.hca_plate_compound pc where pc.plate_id = ?"
				+ "		and (select count(w.well_id) from phaedra.hca_plate_well w where w.platecompound_id = pc.platecompound_id) > 3) crc_count,"
				+ " (select count(pc.platecompound_id) from phaedra.hca_plate_compound pc where pc.plate_id = ?"
				+ "		and (select count(w.well_id) from phaedra.hca_plate_well w where w.platecompound_id = pc.platecompound_id) <= 3) screen_count"
				+ JDBCUtils.getFromDual();
		sql = sql.replace("?", "" + plate.getId());

		PreparedStatement ps = null;
		try (Connection conn = Screening.getEnvironment().getJDBCConnection()) {
			ps = conn.prepareStatement(sql);
			ResultSet resultSet = ps.executeQuery();

			if (resultSet.next()) {
				summary.crcCount = resultSet.getInt(1);
				summary.screenCount = resultSet.getInt(2);
			}
		} catch (SQLException e) {
			EclipseLog.error(e.getMessage(), e, Activator.getDefault());
		} finally {
			if (ps != null) try { ps.close(); } catch (SQLException e) {};
		}

		return summary;
	}

	/**
	 * Get the path to a plate's image, relative to the file server root.
	 * 
	 * @param plate The plate to get the path for.
	 * @return The path to the plate's image file, relative to the file server root.
	 */
	public String getImageFSPath(Plate plate) {
		if (!plate.isImageAvailable()) return null;
		for (String fmt: CodecFactory.getSupportedFormats()) {
			String fileName = plate.getId() + "." + fmt;
			String imagePath = getPlateFSPath(plate) + "/" + fileName;
			try {
				if (Screening.getEnvironment().getFileServer().exists(imagePath)) return imagePath;
			} catch (IOException e) {}
		}
		return null;
	}

	/**
	 * Get the path to a plate's data folder, relative to the file server root.
	 * 
	 * @param plate The plate to get the path for.
	 * @return The path to the plate's data folder, relative to the file server root.
	 */
	public String getPlateFSPath(Plate plate) {
		if (plate == null) return null;
		if (plate.getImagePath() == null) {
			String imagePath = PlateUtils.getProtocolClass(plate).getId() + "/" + FileUtils.createYearWeekString();
			plate.setImagePath(imagePath);
			save(plate);
		}

		StringBuilder sb = new StringBuilder();
		sb.append("/plate.data/");
		sb.append(plate.getImagePath());
		if (!plate.getImagePath().endsWith("/")) sb.append("/");
		sb.append(plate.getId());
		return sb.toString();
	}

	/**
	 * Get all the key-value properties of a plate.
	 * 
	 * @param plate The plate whose properties will be retrieved.
	 * @return A map of key-value properties of the plate.
	 */
	@SuppressWarnings("unchecked")
	public Map<String, String> getPlateProperties(Plate plate) {
		ICache cache = CacheService.getInstance().getDefaultCache();
		String cacheKey = "plateProperties" + plate.getId();
		Map<String, String> properties = (Map<String, String>) cache.get(cacheKey);
		if (properties == null) {
			properties = platePropertyDAO.getAllProperties(plate);
			cache.put(cacheKey, properties);
		}
		return properties;
	}

	/**
	 * Retrieve a single plate property.
	 * 
	 * @param plate The plate to retrieve a property for.
	 * @param name The name of the property (case sensitive).
	 * @return The value of the property, or null if the plate does not have the given property.
	 */
	public String getPlateProperty(Plate plate, String name) {
		return getPlateProperties(plate).get(name);
	}

	/**
	 * Save a single property value for a plate.
	 * 
	 * @param plate The plate to save a property for.
	 * @param name The name of the property.
	 * @param value The value of the property.
	 */
	public void setPlateProperty(Plate plate, String name, String value) {
		SecurityService.getInstance().checkWithException(Permissions.PLATE_EDIT, plate);
		getPlateProperties(plate).put(name, value);
		platePropertyDAO.setProperty(plate, name, value);
	}

	/**
	 * Save a set of properties for a plate.
	 * 
	 * @param plate The plate to save properties for.
	 * @param props The map of key-value properties to save.
	 */
	public void setPlateProperties(Plate plate, Map<String,String> props) {
		SecurityService.getInstance().checkWithException(Permissions.PLATE_EDIT, plate);
		getPlateProperties(plate).clear();
		getPlateProperties(plate).putAll(props);
		platePropertyDAO.setProperties(plate, props);
	}

	/**
	 * Get a list of all compounds that match the given type and number.
	 * 
	 * @param type The compound type.
	 * @param nr The compound number.
	 * @return A list of matching compounds.
	 */
	public List<Compound> getCompounds(String type, String nr) {
		String query = "select c from Compound c where c.type = ?1 and c.number = ?2";
		return streamableList(getList(query, Compound.class, type, nr)).stream()
				.filter(c -> SecurityService.getInstance().check(Permissions.PLATE_OPEN, c))
				.collect(Collectors.toList());
	}

	/**
	 * Create a new compound. Make sure to call {@link PlateService#saveCompounds(Plate)} afterwards.
	 * 
	 * @param plate The plate to create the compound in.
	 * @param type The compound type.
	 * @param number The compound number.
	 * @return The newly created compound, not yet saved.
	 */
	public Compound createCompound(Plate plate, String type, String number) {
		Compound compound = new Compound();
		compound.setType(type);
		compound.setNumber(number);
		compound.setPlate(plate);
		compound.setWells(new ArrayList<>());
		plate.getCompounds().add(compound);
		return compound;
	}

	/**
	 * Save the compounds in a plate.
	 * This will automatically remove compounds that are no longer referenced by any well in the plate.
	 * 
	 * @param plate The plate whose compounds should be saved.
	 */
	public void saveCompounds(Plate plate) {
		SecurityService.getInstance().checkWithException(Permissions.PLATE_EDIT, plate);
		List<Compound> orphanCompounds = streamableList(plate.getCompounds()).stream().filter(c -> c.getWells().isEmpty()).collect(Collectors.toList());
		plate.getCompounds().removeAll(orphanCompounds);
		saveCollection(plate.getCompounds());
		for (Compound c: orphanCompounds) delete(c);
	}
	
	/**
	 * Update the validation status of a compound.
	 * For changes to the compound itself, use {@link PlateService#saveCompounds(Plate)} instead.
	 * 
	 * @param compound The compound to update.
	 */
	public void updateCompound(Compound compound) {
		SecurityService.getInstance().checkWithException(Permissions.PLATE_EDIT, compound.getPlate());
		save(compound);
	}

	/**
	 * Retrieve a well by its primary ID.
	 * 
	 * @param wellId The ID of the well.
	 * @return The well, or null if no matching well was found.
	 */
	public Well getWellById(long wellId) {
		Well well = getEntity(Well.class, wellId);
		if (!SecurityService.getInstance().check(Permissions.PLATE_OPEN, well)) return null;
		return well;
	}
	
	/**
	 * See {@link PlateService#getWellData(List, List))}
	 * 
	 * @param wells The wells whose data should be retrieved.
	 * @return A list of well data for the given wells and feature.
	 */
	public List<FeatureValue> getWellData(List<Well> wells, Feature feature) {
		return featureValueDAO.getValues(wells, feature);
	}

	/**
	 * See {@link PlateService#getWellData(List, List))}
	 * 
	 * @param well The well whose data should be retrieved.
	 * @return A list of well data for the given well and features.
	 */
	public List<FeatureValue> getWellData(Well well, List<Feature> features) {
		return featureValueDAO.getValues(well, features);
	}

	/**
	 * Retrieve the well data for a list of wells, for a list of well features.
	 * <p>
	 * Note that this data is <b>not</b> cached. This is a low-level method that 
	 * should not be called directly.
	 * </p>
	 * 
	 * @param wells The wells whose well data should be retrieved.
	 * @param features The features to retrieve data for.
	 * @return A list of well data for the given wells and features.
	 */
	public List<FeatureValue> getWellData(List<Well> wells, List<Feature> features) {
		return featureValueDAO.getValues(wells, features);
	}

	/**
	 * See {@link PlateService#getWellData(Plate, List)}
	 * 
	 * @param plate The plate whose well data should be retrieved.
	 * @return A list of well data for the given plate and features.
	 */
	public List<FeatureValue> getWellData(Plate plate) {
		return featureValueDAO.getValues(plate);
	}
	
	/**
	 * Retrieve the well data for all wells of a plate, for a list of well features.
	 * <p>
	 * Note that this data is <b>not</b> cached. This is a low-level method that 
	 * should not be called directly.
	 * </p>
	 * 
	 * @param plate The plate whose well data should be retrieved.
	 * @param features The features to retrieve data for.
	 * @return A list of well data for the given plate and features.
	 */
	public List<FeatureValue> getWellData(Plate plate, List<Feature> features) {
		return featureValueDAO.getValues(plate, features);
	}

	/**
	 * See {@link PlateService#updateWellDataRaw(Plate, Feature, double[], boolean)}
	 * 
	 * @param plate The plate whose well data should be updated.
	 * @param feature The feature to update data for.
	 * @param rawValues The raw numeric values that should be saved.
	 */
	public void updateWellDataRaw(Plate plate, Feature feature, double[] rawValues) {
		updateWellDataRaw(plate, feature, rawValues, false);
	}
	
	/**
	 * Update the numeric well data for a given plate and feature.
	 * 
	 * @param plate The plate whose well data should be updated.
	 * @param feature The feature to update data for.
	 * @param rawValues The raw numeric values that should be saved.
	 * @param newPlate True if this plate does not yet have raw data for the given feature.
	 * This is faster because no check needs to be made for existing values to replace.
	 */
	public void updateWellDataRaw(Plate plate, Feature feature, double[] rawValues, boolean newPlate) {
		SecurityService.getInstance().checkWithException(Permissions.PLATE_EDIT, plate);
		WellDataActionHookManager.preAction(plate, feature, rawValues, false, ModelEventType.ObjectChanged);
		if (newPlate) featureValueDAO.insertValues(plate, feature, rawValues, null, null);
		else featureValueDAO.updateValues(plate, feature, rawValues, null, null);
		WellDataActionHookManager.postAction(plate, feature, rawValues, false, ModelEventType.ObjectChanged);
	}
	
	/**
	 * See {@link PlateService#updateWellDataRaw(Plate, Feature, String[], boolean)}
	 * 
	 * @param plate The plate whose well data should be updated.
	 * @param feature The feature to update data for.
	 * @param stringValues The String values that should be saved.
	 */
	public void updateWellDataRaw(Plate plate, Feature feature, String[] stringValues) {
		updateWellDataRaw(plate, feature, stringValues, false);
	}
	
	/**
	 * Update the String well data for a given plate and feature.
	 * 
	 * @param plate The plate whose well data should be updated.
	 * @param feature The feature to update data for.
	 * @param stringValues The String values that should be saved.
	 * @param newPlate True if this plate does not yet have data for the given feature.
	 * This is faster because no check needs to be made for existing values to replace.
	 */
	public void updateWellDataRaw(Plate plate, Feature feature, String[] stringValues, boolean newPlate) {
		SecurityService.getInstance().checkWithException(Permissions.PLATE_EDIT, plate);
		WellDataActionHookManager.preAction(plate, feature, stringValues, false, ModelEventType.ObjectChanged);
		if (newPlate) featureValueDAO.insertValues(plate, feature, null, stringValues, null);
		else featureValueDAO.updateValues(plate, feature, null, stringValues, null);
		WellDataActionHookManager.postAction(plate, feature, stringValues, false, ModelEventType.ObjectChanged);
	}
	
	/**
	 * Update the normalized numeric well data for a given plate and feature.
	 * 
	 * @param plate The plate whose well data should be updated.
	 * @param feature The feature to update data for.
	 * @param normValues The normalized values that should be saved.
	 */
	public void updateWellDataNorm(Plate plate, Feature feature, double[] normValues) {
		SecurityService.getInstance().checkWithException(Permissions.PLATE_EDIT, plate);
		WellDataActionHookManager.preAction(plate, feature, normValues, true, ModelEventType.ObjectChanged);
		featureValueDAO.updateValues(plate, feature, null, null, normValues);
		WellDataActionHookManager.postAction(plate, feature, normValues, true, ModelEventType.ObjectChanged);
	}

	protected void fire(ModelEventType type, Object object, int status) {
		ModelEvent event = new ModelEvent(object, type, status);
		ModelEventService.getInstance().fireEvent(event);
	}

	@Override
	protected void afterSave(Object o) {
		//TODO If object is new, fire ObjectCreated instead.
		fire(ModelEventType.ObjectChanged, o, 0);
	}

	@Override
	protected void beforeDelete(Object o) {
		fire(ModelEventType.ObjectAboutToBeRemoved, o, 0);
	}

	@Override
	protected void afterDelete(Object o) {
		fire(ModelEventType.ObjectRemoved, o, 0);
	}
}
