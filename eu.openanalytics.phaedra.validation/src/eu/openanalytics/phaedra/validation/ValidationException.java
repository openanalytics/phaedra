package eu.openanalytics.phaedra.validation;

public class ValidationException extends RuntimeException {

	private static final long serialVersionUID = 6275361926831887952L;

	public ValidationException(String msg) {
		super(msg);
	}
	
	public ValidationException(String msg, Throwable cause) {
		super(msg, cause);
	}
	
	public ValidationException(Throwable cause) {
		super(cause);
	}
}
