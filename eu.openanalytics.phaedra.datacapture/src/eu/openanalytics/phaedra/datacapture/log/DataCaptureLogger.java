package eu.openanalytics.phaedra.datacapture.log;

import eu.openanalytics.phaedra.datacapture.DataCaptureContext;
import eu.openanalytics.phaedra.datacapture.DataCaptureService;
import eu.openanalytics.phaedra.datacapture.DataCaptureTask;
import eu.openanalytics.phaedra.datacapture.log.DataCaptureLogItem.LogItemSeverity;
import eu.openanalytics.phaedra.datacapture.model.PlateReading;

/**
 * Convenience class for sending data capture log events.
 * Forwards the events to the DataCaptureService, which distributes them to all listeners.
 */
public class DataCaptureLogger {

	private DataCaptureContext context;
	
	public DataCaptureLogger(DataCaptureContext context) {
		this.context = context;
	}
	
	public void started(DataCaptureTask task) {
		DataCaptureLogItem item = new DataCaptureLogItem(LogItemSeverity.Started, context.getTask(), null, null, "Data capture started", null);
		DataCaptureService.getInstance().fireLogEvent(item);
	}

	public void completed(PlateReading reading, String readingSourceId) {
		DataCaptureLogItem item = new DataCaptureLogItem(LogItemSeverity.Completed, context.getTask(), null, reading, "Reading captured", null);
		item.readingSourceId = readingSourceId;
		DataCaptureService.getInstance().fireLogEvent(item);
	}
	
	public void completed(DataCaptureTask task) {
		DataCaptureLogItem item = new DataCaptureLogItem(LogItemSeverity.Completed, context.getTask(), null, null, "Data capture completed", null);
		DataCaptureService.getInstance().fireLogEvent(item);
	}
	
	public void cancelled(DataCaptureTask task) {
		DataCaptureLogItem item = new DataCaptureLogItem(LogItemSeverity.Cancelled, context.getTask(), null, null, "Data capture cancelled", null);
		DataCaptureService.getInstance().fireLogEvent(item);
	}
	
	public void info(String msg) {
		info(null, msg);
	}
	
	public void info(PlateReading reading, String msg) {
		DataCaptureLogItem item = new DataCaptureLogItem(LogItemSeverity.Info, context.getTask(), context.getActiveModule(), reading, msg, null);
		DataCaptureService.getInstance().fireLogEvent(item);
	}

	public void warn(String msg) {
		warn(null, msg);
	}
	
	public void warn(PlateReading reading, String msg) {
		DataCaptureLogItem item = new DataCaptureLogItem(LogItemSeverity.Warning, context.getTask(), context.getActiveModule(), reading, msg, null);
		DataCaptureService.getInstance().fireLogEvent(item);
	}
	
	public void error(String msg) {
		error(null, msg, null);
	}
	
	public void error(PlateReading reading, String msg) {
		error(reading, msg, null);
	}
	
	public void error(PlateReading reading, String msg, Throwable cause) {
		DataCaptureLogItem item = new DataCaptureLogItem(LogItemSeverity.Error, context.getTask(), context.getActiveModule(), reading, msg, cause);
		DataCaptureService.getInstance().fireLogEvent(item);
	}
}
