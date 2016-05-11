package eu.openanalytics.phaedra.link.platedef.link;

public class PlateLinkException extends Exception {

	private static final long serialVersionUID = -5926414462404951779L;

	public PlateLinkException(String msg) {
		super(msg);
	}
	
	public PlateLinkException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
