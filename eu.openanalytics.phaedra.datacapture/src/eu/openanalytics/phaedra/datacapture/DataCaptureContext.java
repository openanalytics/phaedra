package eu.openanalytics.phaedra.datacapture;

import java.util.HashMap;
import java.util.Map;

import eu.openanalytics.phaedra.datacapture.config.ParameterGroup;
import eu.openanalytics.phaedra.datacapture.log.DataCaptureLogger;
import eu.openanalytics.phaedra.datacapture.model.PlateReading;
import eu.openanalytics.phaedra.datacapture.module.IModule;
import eu.openanalytics.phaedra.datacapture.store.DefaultDataCaptureStore;
import eu.openanalytics.phaedra.datacapture.store.IDataCaptureStore;

/**
 * This context keeps track of plate readings generated during
 * a data capture job. Each reading must be instantiated using
 * a unique number (e.g. plate sequence or file part).
 * 
 * During a capture job, all modules have access to the context.
 */
public class DataCaptureContext {

	private Map<Integer, PlateReading> readings;
	private Map<Integer, IDataCaptureStore> stores;
	private Map<Integer, String> readingSourceIds;
	private Map<Integer, ParameterGroup> readingParameters;
	
	private DataCaptureTask task;
	private DataCaptureLogger logger;
	
	private IModule activeModule;
	private PlateReading activeReading;
	
	public DataCaptureContext(DataCaptureTask task) {
		this.readings = new HashMap<>();
		this.stores = new HashMap<>();
		this.readingSourceIds = new HashMap<>();
		this.readingParameters = new HashMap<>();
		this.task = task;
		this.logger = new DataCaptureLogger(this);
	}
	
	public PlateReading getReading(int nr) {
		return readings.get(nr);
	}
	
	public IDataCaptureStore getStore(int nr) {
		return stores.get(nr);
	}
	
	public String getReadingSourceId(PlateReading reading) {
		for (Integer i: readings.keySet()) {
			if (readings.get(i) == reading) return readingSourceIds.get(i);
		}
		return null;
	}
	
	public void updateReadingSourceId(PlateReading reading, String sourceId) {
		for (Integer i: readings.keySet()) {
			if (readings.get(i) == reading) readingSourceIds.put(i, sourceId);
		}
	}
	
	public IDataCaptureStore getStore(PlateReading reading) {
		for (Integer i: readings.keySet()) {
			if (readings.get(i) == reading) return stores.get(i);
		}
		return null;
	}
	
	public ParameterGroup getParameters(PlateReading reading) {
		for (Integer i: readings.keySet()) {
			if (readings.get(i) == reading)  return readingParameters.get(i);
		}
		return null;
	}
	
	public PlateReading createNewReading(int nr) throws DataCaptureException {
		return createNewReading(nr, null);
	}
	
	public PlateReading createNewReading(int nr, String sourceId) throws DataCaptureException {
		PlateReading reading = DataCaptureService.getInstance().createReading();
		reading.setUser(task.getUser());
		readings.put(nr, reading);
		readingSourceIds.put(nr, sourceId);
		readingParameters.put(nr, new ParameterGroup());
		IDataCaptureStore dataStore = new DefaultDataCaptureStore();
		dataStore.initialize(reading);
		stores.put(nr, dataStore);
		
		return reading;
	}
	
	public PlateReading[] getReadings() {
		return readings.values().toArray(new PlateReading[readings.size()]);
	}
	
	public int getReadingCount() {
		return readings.keySet().size();
	}
	
	public DataCaptureLogger getLogger() {
		return logger;
	}
	
	public DataCaptureTask getTask() {
		return task;
	}
	
	public IModule getActiveModule() {
		return activeModule;
	}
	
	public void setActiveModule(IModule activeModule) {
		this.activeModule = activeModule;
	}
	
	public PlateReading getActiveReading() {
		return activeReading;
	}
	
	public void setActiveReading(PlateReading activeReading) {
		this.activeReading = activeReading;
	}
}
