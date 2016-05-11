package eu.openanalytics.phaedra.datacapture.log;

import java.util.Date;

import org.eclipse.swt.graphics.Image;

import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.datacapture.DataCaptureTask;
import eu.openanalytics.phaedra.datacapture.model.PlateReading;
import eu.openanalytics.phaedra.datacapture.module.IModule;

public class DataCaptureLogItem {

	public LogItemSeverity severity;
	public DataCaptureTask task;
	public IModule module;
	public PlateReading reading;
	public String readingSourceId;
	public String message;
	public Throwable errorCause;
	public Date timestamp;
	public String logSource;
	
	public DataCaptureLogItem() {
		this.timestamp = new Date();
	}
	
	public DataCaptureLogItem(String logSource, int logLevel, DataCaptureTask task, String msg, Throwable errorCause) {
		this();
		this.logSource = logSource;
		this.severity = LogItemSeverity.getLogItem(logLevel);
		this.task = task;
		this.message = msg;
		this.errorCause = errorCause;
	}
	
	public DataCaptureLogItem(LogItemSeverity severity, DataCaptureTask task, IModule module, PlateReading reading, String msg, Throwable errorCause) {
		this();
		this.severity = severity;
		this.task = task;
		this.module = module;
		this.reading = reading;
		this.message = msg;
		this.errorCause = errorCause;
	}
	
	public static enum LogItemSeverity {
		Started("information.png", 0),
		Info("information.png", 999),
		Warning("error.png", 998),
		Error("exclamation.png", -1),
		Completed("information.png", 1),
		Cancelled("information.png", -2);
		
		private String icon;
		private int logLevel;
		
		private LogItemSeverity(String icon, int logLevel) {
			this.icon = icon;
			this.logLevel = logLevel;
		}
		
		public static LogItemSeverity getLogItem(int logLevel) {
			for (LogItemSeverity sev: values()) {
				if (sev.logLevel == logLevel) return sev;
			}
			return null;
		}
		
		public Image getIcon() {
			return IconManager.getIconImage(icon);
		}
		
		public int getLogLevel() {
			return logLevel;
		}
		
		public boolean isPersistent() {
			return logLevel < 900;
		}
	}
}
