package eu.openanalytics.phaedra.datacapture.parser;

public class ParseException extends Exception {

	private static final long serialVersionUID = 8833624461971294669L;

	public ParseException() {
		super();
	}
	
	public ParseException(String msg) {
		super(msg);
	}
	
	public ParseException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
