package eu.openanalytics.phaedra.datacapture.scanner;

public class ScanException extends Exception {

	private static final long serialVersionUID = -8983263480407712394L;

	public ScanException(String msg) {
		super(msg);
	}
	
	public ScanException(String msg, Throwable cause) {
		super(msg, cause);
	}
	
}
