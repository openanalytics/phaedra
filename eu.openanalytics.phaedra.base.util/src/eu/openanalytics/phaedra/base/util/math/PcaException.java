package eu.openanalytics.phaedra.base.util.math;

public class PcaException extends Exception {	
	private static final long serialVersionUID = 1L;

	public PcaException() {
		super();
	}
	
	public PcaException(String message) {
		super(message);
	}
	
	public PcaException(String message, Throwable cause) {
		super(message,cause);
	}
	
	public PcaException(Throwable cause) {
		super(cause);
	}
	
	
}