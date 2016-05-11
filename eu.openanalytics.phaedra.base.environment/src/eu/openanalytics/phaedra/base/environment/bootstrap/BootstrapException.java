package eu.openanalytics.phaedra.base.environment.bootstrap;

public class BootstrapException extends Exception {

	private static final long serialVersionUID = 7765560852247085305L;

	public BootstrapException(String msg) {
		super(msg);
	}
	
	public BootstrapException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
