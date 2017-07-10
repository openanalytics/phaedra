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
 * This service controls the creation, retrieval and modification of Plates and their related objects:
 * Experiments, Wells, Compounds, well data.
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

	/*
	 * **********
	 * Public API
	 * **********
	 */

	/* Experiments
	 * ***********
	 */

	/**
	 * Get a list of all experiments in the given protocol that are visible to the current user.
	 */
	public List<Experiment> getExperiments(Protocol protocol) {
		if (!SecurityService.getInstance().check(Permissions.PROTOCOL_OPEN, protocol)) return new ArrayList<>();
		String query = "select e from Experiment e where e.protocol = ?1";
		return streamableList(getList(query, Experiment.class, protocol));
	}

	/**
	 * Get an experiment using its id, if the current user has access to it.
	 */
	public Experiment getExperiment(long experimentId) {
		Experiment exp = getEntity("select e from Experiment e where e.id = ?1", Experiment.class, experimentId);
		if (!SecurityService.getInstance().check(Permissions.EXPERIMENT_OPEN, exp)) return null;
		return exp;
	}

	public void deleteExperiment(Experiment experiment) {
		SecurityService.getInstance().checkWithException(Permissions.EXPERIMENT_DELETE, experiment);
		List<Plate> plates = getPlates(experiment);
		deletePlates(plates);
		delete(experiment);
	}

	public void updateExperiment(Experiment experiment) {
		SecurityService.getInstance().checkWithException(Permissions.EXPERIMENT_EDIT, experiment);
		if (experiment.getName() == null) throw new IllegalArgumentException("Experiment name cannot be null.");
		save(experiment);
	}

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

	public void moveExperiments(List<Experiment> experiments, Protocol newProtocol) {
		SecurityService.getInstance().checkWithException(Permissions.EXPERIMENT_CREATE, newProtocol);
		for (Experiment experiment: experiments) {
			SecurityService.getInstance().checkWithException(Permissions.EXPERIMENT_MOVE, experiment);
			experiment.setProtocol(newProtocol);
		}
		saveCollection(experiments);
	}

	public ExperimentSummary getExperimentSummary(Experiment exp) {
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
				+ " from dual";
		sql = sql.replace("?", "" + exp.getId());

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

	/* Plates
	 * ******
	 */

	/**
	 * Get a list of all plates in the given experiment, if the user has access to them.
	 */
	public List<Plate> getPlates(Experiment exp) {
		if (!SecurityService.getInstance().check(Permissions.EXPERIMENT_OPEN, exp)) return new ArrayList<>();
		return streamableList(getList("select p from Plate p where p.experiment = ?1", Plate.class, exp));
	}

	/**
	 * Get a list of all plates that match the given barcode, and are visible to the current user.
	 */
	public List<Plate> getPlates(String barcodePattern) {
		String query = "select p from Plate p where p.barcode like ?1";
		barcodePattern = "%" + barcodePattern + "%";
		return streamableList(getList(query, Plate.class, barcodePattern)).stream()
				.filter(p -> SecurityService.getInstance().check(Permissions.PLATE_OPEN, p))
				.collect(Collectors.toList());
	}

	/**
	 * Get a list of all plates in the given protocol class that match the given barcode, and are visible to the current user.
	 */
	public List<Plate> getPlates(String barcodePattern, ProtocolClass pClass) {
		String query = "select p from Plate p where p.barcode like ?1 and p.experiment.protocol.protocolClass = ?2";
		barcodePattern = "%" + barcodePattern + "%";
		return streamableList(getList(query, Plate.class, barcodePattern, pClass)).stream()
				.filter(p -> SecurityService.getInstance().check(Permissions.PLATE_OPEN, p))
				.collect(Collectors.toList());
	}

	/**
	 * Get a plate by its id, if the current user has access to it.
	 */
	public Plate getPlateById(long plateId) {
		Plate plate = getEntity(Plate.class, plateId);
		if (!SecurityService.getInstance().check(Permissions.PLATE_OPEN, plate)) return null;
		return plate;
	}
	
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
	 * Modify the properties of a plate. Note that the permission needed to modify a plate
	 * depends on the plate's validation and approval status.
	 * To modify an approved plate's validation or approval status, use {@link updatePlateValidation} instead.
	 */
	public void updatePlate(Plate plate) {
		updatePlate(plate, ModelEventType.ObjectChanged);
	}

	/**
	 * Modify a validation-related field of a plate, well or compound.
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
	 * Create an copy of a plate, including its well data, image data and subwell data.
	 * The following properties are NOT copied:
	 * <ul>
	 * <li>Validation and approval status</li>
	 * <li>Curve fits and manual curve settings</li>
	 * <li>Validation history</li>
	 * </ul>
	 * The plate is recalculated immediately after the copy.
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

	public void deletePlate(Plate plate) {
		PlateActionHookManager.preAction(plate, ModelEventType.ObjectRemoved);

		SecurityService.getInstance().checkWithException(Permissions.PLATE_DELETE, plate);

		// Delete the plate files (image and subwell data)
		String plateFSPath = null;
		try {
			plateFSPath = getPlateFSPath(plate);
			Screening.getEnvironment().getFileServer().delete(plateFSPath + "/");
		} catch (IOException e) {
			Activator.getDefault().getLog().log(new Status(IStatus.ERROR, Activator.PLUGIN_ID,
					"Failed to clean up plate files at " + plateFSPath, e));
		}

		// Delete the plate object (cascades to wells, compounds, feature values and curve results)
		delete(plate);

		PlateActionHookManager.postAction(plate, ModelEventType.ObjectRemoved);
	}

	public PlateSummary getPlateSummary(Plate plate) {
		PlateSummary summary = new PlateSummary();

		String sql = "select"
				+ " (select count(pc.platecompound_id) from phaedra.hca_plate_compound pc where pc.plate_id = ?"
				+ "		and (select count(w.well_id) from phaedra.hca_plate_well w where w.platecompound_id = pc.platecompound_id) > 3) crc_count,"
				+ " (select count(pc.platecompound_id) from phaedra.hca_plate_compound pc where pc.plate_id = ?"
				+ "		and (select count(w.well_id) from phaedra.hca_plate_well w where w.platecompound_id = pc.platecompound_id) <= 3) screen_count"
				+ " from dual";
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
	 * Get a full or relative path to a plate's image file.
	 * A relative path is only useful for the SecureFileServer, because the path is relative
	 * to the file server root.
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
	 * Get a full or relative path to a plate's file server folder.
	 * A relative path is only useful for the SecureFileServer, because the path is relative
	 * to the file server root.
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

	public String getPlateProperty(Plate plate, String name) {
		return getPlateProperties(plate).get(name);
	}

	public void setPlateProperty(Plate plate, String name, String value) {
		SecurityService.getInstance().checkWithException(Permissions.PLATE_EDIT, plate);
		getPlateProperties(plate).put(name, value);
		platePropertyDAO.setProperty(plate, name, value);
	}

	public void setPlateProperties(Plate plate, Map<String,String> props) {
		SecurityService.getInstance().checkWithException(Permissions.PLATE_EDIT, plate);
		getPlateProperties(plate).clear();
		getPlateProperties(plate).putAll(props);
		platePropertyDAO.setProperties(plate, props);
	}

	/* Compounds
	 * *********
	 */

	/**
	 * Get a list of all compounds that match a given type and number, and that are contained
	 * in plates that are visible to the current user.
	 */
	public List<Compound> getCompounds(String type, String nr) {
		String query = "select c from Compound c where c.type = ?1 and c.number = ?2";
		return streamableList(getList(query, Compound.class, type, nr)).stream()
				.filter(c -> SecurityService.getInstance().check(Permissions.PLATE_OPEN, c))
				.collect(Collectors.toList());
	}

	public Compound createCompound(Plate plate, String type, String number) {
		Compound compound = new Compound();
		compound.setType(type);
		compound.setNumber(number);
		compound.setPlate(plate);
		compound.setWells(new ArrayList<>());
		plate.getCompounds().add(compound);
		return compound;
	}

	public void saveCompounds(Plate plate) {
		SecurityService.getInstance().checkWithException(Permissions.PLATE_EDIT, plate);
		List<Compound> orphanCompounds = streamableList(plate.getCompounds()).stream().filter(c -> c.getWells().isEmpty()).collect(Collectors.toList());
		plate.getCompounds().removeAll(orphanCompounds);
		saveCollection(plate.getCompounds());
		for (Compound c: orphanCompounds) delete(c);
	}
	
	public void updateCompound(Compound c) {
		SecurityService.getInstance().checkWithException(Permissions.PLATE_EDIT, c.getPlate());
		save(c);
	}

	/* Well data
	 * *********
	 */

	public List<FeatureValue> getWellData(List<Well> wells, Feature feature) {
		return featureValueDAO.getValues(wells, feature);
	}

	public List<FeatureValue> getWellData(Well well, List<Feature> features) {
		return featureValueDAO.getValues(well, features);
	}

	public List<FeatureValue> getWellData(List<Well> wells, List<Feature> features) {
		return featureValueDAO.getValues(wells, features);
	}

	public List<FeatureValue> getWellData(Plate plate, List<Feature> features) {
		return featureValueDAO.getValues(plate, features);
	}

	public List<FeatureValue> getWellData(Plate plate) {
		return featureValueDAO.getValues(plate);
	}

	public void updateWellDataRaw(Plate plate, Feature feature, double[] rawValues) {
		updateWellDataRaw(plate, feature, rawValues, false);
	}
	
	public void updateWellDataRaw(Plate plate, Feature feature, double[] rawValues, boolean newPlate) {
		SecurityService.getInstance().checkWithException(Permissions.PLATE_EDIT, plate);
		WellDataActionHookManager.preAction(plate, feature, rawValues, false, ModelEventType.ObjectChanged);
		if (newPlate) featureValueDAO.insertValues(plate, feature, rawValues, null, null);
		else featureValueDAO.updateValues(plate, feature, rawValues, null, null);
		WellDataActionHookManager.postAction(plate, feature, rawValues, false, ModelEventType.ObjectChanged);
	}
	
	public void updateWellDataRaw(Plate plate, Feature feature, String[] stringValues) {
		updateWellDataRaw(plate, feature, stringValues, false);
	}
	
	public void updateWellDataRaw(Plate plate, Feature feature, String[] stringValues, boolean newPlate) {
		SecurityService.getInstance().checkWithException(Permissions.PLATE_EDIT, plate);
		WellDataActionHookManager.preAction(plate, feature, stringValues, false, ModelEventType.ObjectChanged);
		if (newPlate) featureValueDAO.insertValues(plate, feature, null, stringValues, null);
		else featureValueDAO.updateValues(plate, feature, null, stringValues, null);
		WellDataActionHookManager.postAction(plate, feature, stringValues, false, ModelEventType.ObjectChanged);
	}
	
	public void updateWellDataNorm(Plate plate, Feature feature, double[] normValues) {
		SecurityService.getInstance().checkWithException(Permissions.PLATE_EDIT, plate);
		WellDataActionHookManager.preAction(plate, feature, normValues, true, ModelEventType.ObjectChanged);
		featureValueDAO.updateValues(plate, feature, null, null, normValues);
		WellDataActionHookManager.postAction(plate, feature, normValues, true, ModelEventType.ObjectChanged);
	}

	/*
	 * **************
	 * Event handling
	 * **************
	 */

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
