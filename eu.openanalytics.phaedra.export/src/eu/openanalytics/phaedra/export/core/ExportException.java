package eu.openanalytics.phaedra.export.core;

public class ExportException extends Exception {

	private static final long serialVersionUID = 126856378002151430L;

	public ExportException(String msg) {
		super(msg);
	}
	
	public ExportException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
