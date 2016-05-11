package eu.openanalytics.phaedra.calculation.norm;

public class NormalizationException extends RuntimeException {

	private static final long serialVersionUID = 4841145152396496962L;

	public NormalizationException(String msg) {
		super(msg);
	}
	
	public NormalizationException(String msg, Throwable cause) {
		super(msg, cause);
	}
	
	public NormalizationException(Throwable cause) {
		super(cause);
	}
}
