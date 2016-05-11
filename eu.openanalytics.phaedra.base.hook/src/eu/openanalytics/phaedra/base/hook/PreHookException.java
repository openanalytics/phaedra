package eu.openanalytics.phaedra.base.hook;

public class PreHookException extends Exception {

	private static final long serialVersionUID = 148160344994726449L;

	public PreHookException() {
		super();
	}
	
	public PreHookException(String message) {
		super(message);
	}
	
	public PreHookException(String message, Throwable cause) {
		super(message, cause);
	}
}
