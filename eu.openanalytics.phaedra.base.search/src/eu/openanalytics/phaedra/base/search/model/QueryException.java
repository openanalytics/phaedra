package eu.openanalytics.phaedra.base.search.model;

public class QueryException extends Exception {	
	private static final long serialVersionUID = 6869898974305079859L;

	public QueryException() {
		super();
	}

	public QueryException(String message, Throwable cause) {
		super(message, cause);
	}

	public QueryException(String message) {
		super(message);
	}

	public QueryException(Throwable cause) {
		super(cause);
	}
}
