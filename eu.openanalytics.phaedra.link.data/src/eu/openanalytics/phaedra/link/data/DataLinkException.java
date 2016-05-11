package eu.openanalytics.phaedra.link.data;

public class DataLinkException extends Exception {

	private static final long serialVersionUID = -6138544630831420324L;

	public DataLinkException(String msg) {
		super(msg);
	}
	
	public DataLinkException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
