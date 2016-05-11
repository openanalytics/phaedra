package eu.openanalytics.phaedra.calculation;

public class CalculationException extends RuntimeException {

	private static final long serialVersionUID = -8457963883017546893L;

	public CalculationException(String msg) {
		super(msg);
	}
	
	public CalculationException(String msg, Throwable cause) {
		super(msg, cause);
	}
	
	public CalculationException(Throwable cause) {
		super(cause);
	}
}
