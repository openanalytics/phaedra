package eu.openanalytics.phaedra.model.curve;

public class CurveFitException extends Exception {

	private static final long serialVersionUID = -4190367646338744395L;

	public CurveFitException() {
		super();
	}
	
	public CurveFitException(String msg) {
		super(msg);
	}
	
	public CurveFitException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
