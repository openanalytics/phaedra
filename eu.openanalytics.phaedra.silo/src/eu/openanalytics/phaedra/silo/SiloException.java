package eu.openanalytics.phaedra.silo;

public class SiloException extends Exception {

	private static final long serialVersionUID = -7650328768877701847L;

	public SiloException(String msg) {
		super(msg);
	}
	
	public SiloException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
