package eu.openanalytics.phaedra.base.email;

public class MailException extends Exception {

	private static final long serialVersionUID = -9219259709701265974L;

	public MailException(String msg) {
		super(msg);
	}
	
	public MailException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
