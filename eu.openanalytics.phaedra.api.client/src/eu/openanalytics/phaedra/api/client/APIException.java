package eu.openanalytics.phaedra.api.client;

public class APIException extends RuntimeException {

	private static final long serialVersionUID = -6492823668794080462L;

	public APIException(String message) {
		super(message);
	}
	
	public APIException(String message, Throwable cause) {
		super(message, cause);
	}
}
