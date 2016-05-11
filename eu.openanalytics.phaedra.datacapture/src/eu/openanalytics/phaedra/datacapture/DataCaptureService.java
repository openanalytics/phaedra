package eu.openanalytics.phaedra.datacapture;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.Status;

import eu.openanalytics.phaedra.base.db.JDBCUtils;
import eu.openanalytics.phaedra.base.db.jpa.BaseJPAService;
import eu.openanalytics.phaedra.base.environment.Screening;
import eu.openanalytics.phaedra.base.event.ModelEvent;
import eu.openanalytics.phaedra.base.event.ModelEventService;
import eu.openanalytics.phaedra.base.event.ModelEventType;
import eu.openanalytics.phaedra.base.fs.SecureFileServer;
import eu.openanalytics.phaedra.base.imaging.jp2k.CodecFactory;
import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.base.security.model.Permissions;
import eu.openanalytics.phaedra.base.util.misc.StringUtils;
import eu.openanalytics.phaedra.datacapture.config.CaptureConfig;
import eu.openanalytics.phaedra.datacapture.log.DataCaptureLogItem;
import eu.openanalytics.phaedra.datacapture.log.DataCaptureLogItem.LogItemSeverity;
import eu.openanalytics.phaedra.datacapture.log.IDataCaptureLogListener;
import eu.openanalytics.phaedra.datacapture.model.PlateReading;
import eu.openanalytics.phaedra.datacapture.model.SavedLogEvent;
import eu.openanalytics.phaedra.datacapture.module.ModuleFactory;
import eu.openanalytics.phaedra.datacapture.queue.DataCaptureJobQueue;
import eu.openanalytics.phaedra.datacapture.store.HDF5DataCaptureStore;
import eu.openanalytics.phaedra.datacapture.store.IDataCaptureStore;
import eu.openanalytics.phaedra.datacapture.util.CaptureUtils;


public class DataCaptureService extends BaseJPAService {

	private final static String DC_SERVER_ENABLED = "datacapture.server";
	
	private static DataCaptureService instance = new DataCaptureService();
	
	private boolean serverEnabled;
	private ListenerList logListeners;
	private Map<String, RunningDataCaptureJob> runningJobs;
	
	private DataCaptureService() {
		serverEnabled = Boolean.parseBoolean(System.getProperty(DC_SERVER_ENABLED, "false"));	
		logListeners = new ListenerList();
		runningJobs = new ConcurrentHashMap<>();
		
		if (serverEnabled) checkUnfinishedJobs();
	}
	
	public static DataCaptureService getInstance() {
		return instance;
	}
	
	@Override
	protected EntityManager getEntityManager() {
		return Screening.getEnvironment().getEntityManager();
	}

	public boolean isServerEnabled() {
		return serverEnabled;
	}
	
	public String[] getAllCaptureConfigIds() throws IOException {
		String[] configs = ModuleFactory.getAvailableConfigs();
		Arrays.sort(configs);
		return configs;
	}

	public String getCaptureConfigId(long protocolId) {
		String query = "select pc.default_capture_config from phaedra.hca_protocolclass pc, phaedra.hca_protocol p"
				+ " where pc.protocolclass_id = p.protocolclass_id and p.protocol_id = " + protocolId;
		List<?> res = JDBCUtils.queryWithLock(getEntityManager().createNativeQuery(query), getEntityManager());
		if (res.isEmpty() || res.get(0) == null) return null;
		return res.get(0).toString();
	}

	public CaptureConfig getCaptureConfig(String captureId) throws IOException, DataCaptureException {
		CaptureConfig captureConfig = ModuleFactory.loadCaptureConfig(captureId);
		return captureConfig;
	}
	
	public IDataCaptureStore createDataCaptureStore() {
		return new HDF5DataCaptureStore();
	}
	
	public DataCaptureTask createTask(String source, long protocolId) {
		DataCaptureTask task = createTask(source, getCaptureConfigId(protocolId));
		
		String query = "select protocol_name from phaedra.hca_protocol where protocol_id = " + protocolId;
		List<?> res = JDBCUtils.queryWithLock(getEntityManager().createNativeQuery(query), getEntityManager());
		if (!res.isEmpty() && res.get(0) != null) task.getParameters().put(DataCaptureTask.PARAM_PROTOCOL_NAME, res.get(0).toString());
		
		return task;
	}
	
	public DataCaptureTask createTask(String source, String configId) {
		DataCaptureTask task = new DataCaptureTask();
		task.setId(UUID.randomUUID().toString());
		task.setUser(SecurityService.getInstance().getCurrentUserName());
		task.setSource(source);
		task.setConfigId(configId);
		return task;
	}
	
	public List<PlateReading> executeTask(DataCaptureTask task, IProgressMonitor monitor) throws DataCaptureException {
		DataCapturer capturer = new DataCapturer();
		if (monitor == null) monitor = new DataCaptureProgressMonitor();
		
		runningJobs.put(task.getId(), new RunningDataCaptureJob(task, monitor));
		try {
			return capturer.execute(task, monitor);
		} finally {
			runningJobs.remove(task.getId());
		}
	}
	
	public boolean queueTask(DataCaptureTask task) {
		return DataCaptureJobQueue.submit(task);
	}
	
	public boolean cancelQueuedTask(String taskId) {
		return DataCaptureJobQueue.cancel(taskId);
	}
	
	public RunningDataCaptureJob getRunningJob(String taskId) {
		return runningJobs.get(taskId);
	}
	
	/*
	 * Plate readings
	 */
	
	public PlateReading createReading() {
		PlateReading reading = new PlateReading();
		reading.setDate(new Date());
		reading.setUser(SecurityService.getInstance().getCurrentUserName());
		return reading;
	}
	
	public void updateReading(PlateReading reading) {
		save(reading);
	}
	
	public List<PlateReading> getUnlinkedReadings() {
		String jpql = "select r from PlateReading r where r.linkStatus <= 0";
		List<PlateReading> readings = getList(jpql, PlateReading.class);
		Collections.sort(readings, CaptureUtils.READING_ID_SORTER);
		return readings;
	}
	
	public List<PlateReading> getUnlinkedReadings(String protocol) {
		String jpql = "select r from PlateReading r where r.linkStatus <= 0 and r.protocol = ?1";
		List<PlateReading> readings = getList(jpql, PlateReading.class, protocol);
		return readings;
	}
	
	public List<PlateReading> getAllReadings() {
		List<PlateReading> readings = getList(PlateReading.class);
		Collections.sort(readings, CaptureUtils.READING_ID_SORTER);
		return readings;
	}
	
	public List<PlateReading> getAllReadings(Date from, Date to) {
		if (from == null) from = new Date(0);
		if (to == null) to = new Date();
		String jpql = "SELECT r FROM PlateReading r WHERE r.date >= ?1 AND r.date <= ?2 ORDER BY r.date desc";
		List<PlateReading> readings = getList(jpql, PlateReading.class, from, to);
		return readings;
	}
	
	public PlateReading getReading(long id) {
		String jpql = "SELECT r FROM PlateReading r WHERE r.id = ?1";
		PlateReading reading = getEntity(jpql, PlateReading.class, id);
		return reading;
	}
	
	public String getImagePath(PlateReading reading) throws IOException {
		for (String fmt: CodecFactory.getSupportedFormats()) {
			String imagePath = reading.getCapturePath().replace(".h5", "." + fmt);
			if (Screening.getEnvironment().getFileServer().exists(imagePath)) return imagePath;
		}
		return null;
	}
	
	public void deleteReading(PlateReading reading) {
		SecurityService.getInstance().checkWithException(Permissions.PLATE_DELETE, reading);
		
		//First, delete related files.
		try {
			String hdf5Path = reading.getCapturePath();
			String imagePath = getImagePath(reading);
			SecureFileServer fs = Screening.getEnvironment().getFileServer();
			if (hdf5Path != null && fs.exists(hdf5Path)) fs.delete(hdf5Path);
			if (imagePath != null && fs.exists(imagePath)) fs.delete(imagePath);
		} catch (IOException e) {
			Activator.getDefault().getLog().log(new Status(IStatus.ERROR, Activator.PLUGIN_ID,
					"Failed to delete file(s) for reading " + reading.toString(), e));
		}
		// Then delete the reading object.
		delete(reading);
	}
	
	public boolean isTaskSourceAlreadyCaptured(DataCaptureTask task) {
		// Find a "capture complete" event with the same source path.
		String query = "select e from SavedLogEvent e where e.status = 1 and e.reading is null and e.sourcePath = ?1";
		List<SavedLogEvent> events = getList(query, SavedLogEvent.class, task.getSource());
		return !events.isEmpty();
	}
	
	public boolean isReadingAlreadyCaptured(String readingSourceId) {
		if (readingSourceId == null || readingSourceId.isEmpty()) return false;
		
		String query = "select e from SavedLogEvent e where e.status = 1 and e.reading is not null and e.sourceIdentifier = ?1";
		List<SavedLogEvent> events = getList(query, SavedLogEvent.class, readingSourceId);
		
		// If no reading with this sourceID was previously captured, go ahead.
		if (events.isEmpty()) return false;

		List<String> taskIds = streamableList(events).stream().map(e -> e.getTaskId()).distinct().collect(Collectors.toList());
		for (String taskId: taskIds) {
			query = "select e from SavedLogEvent e where e.status = 1 and e.reading is null and e.taskId = ?1";
			events = getList(query, SavedLogEvent.class, taskId);
			// This reading is part of a successful capture job.
			if (!events.isEmpty()) return true;
		}
		
		return false;
	}
	
	public static class RunningDataCaptureJob {
		
		public DataCaptureTask task;
		public IProgressMonitor monitor;
		
		public RunningDataCaptureJob(DataCaptureTask task, IProgressMonitor monitor) {
			this.task = task;
			this.monitor = monitor;
		}
	}
	
	private void checkUnfinishedJobs() {
		// Find all jobs that have a 'submitted' or 'started' event but not a 'complete', 'error' or 'cancelled' event.
		String query = "select e from SavedLogEvent e where e.status = 0 and e.taskId is not null and not exists"
				+ " (select ee.taskId from SavedLogEvent ee where ee.status in (1,-1,-2) and ee.reading is null and ee.taskId = e.taskId)";
		List<SavedLogEvent> unfinishedJobs = getList(query, SavedLogEvent.class);
		for (SavedLogEvent e: unfinishedJobs) {
			SavedLogEvent errorEvent = new SavedLogEvent();
			errorEvent.setDate(new Date());
			errorEvent.setSource(e.getSource());
			errorEvent.setStatus(LogItemSeverity.Error.getLogLevel());
			errorEvent.setSourceIdentifier(e.getSourceIdentifier());
			errorEvent.setSourcePath(e.getSourcePath());
			errorEvent.setTaskId(e.getTaskId());
			errorEvent.setTaskUser(e.getTaskUser());
			errorEvent.setMessage("The system was stopped while the capture job was queued or running.");
			save(errorEvent);
		}
	}
	
	/*
	 * *******************
	 * Data Capture Events
	 * *******************
	 */
	
	public void addLogListener(IDataCaptureLogListener listener) {
		logListeners.add(listener);
	}
	
	public void removeLogListener(IDataCaptureLogListener listener) {
		logListeners.remove(listener);
	}
	
	public void fireLogEvent(DataCaptureLogItem item) {
		for (Object l: logListeners.getListeners()) {
			((IDataCaptureLogListener)l).logEvent(item);
		}
		
		if (item.severity.isPersistent()) {
			saveLogEvent(item);
		}
	}
	
	private void saveLogEvent(DataCaptureLogItem item) {
		if (!isServerEnabled()) return;
		
		SavedLogEvent event = new SavedLogEvent();
		event.setDate(item.timestamp);
		event.setSource(item.logSource);
		event.setStatus(item.severity.getLogLevel());
		event.setSourceIdentifier(item.readingSourceId);
		if (item.task != null) event.setSourcePath(item.task.getSource());
		if (item.task != null) event.setTaskId(item.task.getId());
		if (item.task != null) event.setTaskUser(item.task.getUser());
		if (item.reading != null) event.setReading(item.reading.toString());
		if (item.errorCause != null) event.setError(StringUtils.trim(StringUtils.getStackTrace(item.errorCause), 2000));
		if (item.message != null) event.setMessage(StringUtils.trim(item.message, 1000));
		save(event);
	}
	
	public List<SavedLogEvent> getSavedEvents(Date from, Date to) {
		if (from == null) from = new Date(0);
		if (to == null) to = new Date();
		String jpql = "SELECT e FROM SavedLogEvent e WHERE e.date >= ?1 AND e.date <= ?2 ORDER BY e.date desc";
		List<SavedLogEvent> items = getList(jpql, SavedLogEvent.class, from, to);
		return items;
	}
	
	public List<SavedLogEvent> getSavedEvents(String taskId) {
		String jpql = "SELECT e FROM SavedLogEvent e WHERE e.taskId = ?1 ORDER BY e.date";
		List<SavedLogEvent> items = getList(jpql, SavedLogEvent.class, taskId);
		return items;
	}
	
	/*
	 * ********************
	 * Model Event handling
	 * ********************
	 */
	
	protected void fire(ModelEventType type, Object object, int status) {
		ModelEvent event = new ModelEvent(object, type, status);
		ModelEventService.getInstance().fireEvent(event);
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
