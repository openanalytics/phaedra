package eu.openanalytics.phaedra.datacapture;

public class DataCaptureException extends Exception {

	private static final long serialVersionUID = -6138544630831420324L;

	public DataCaptureException(String msg) {
		super(msg);
	}
	
	public DataCaptureException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
