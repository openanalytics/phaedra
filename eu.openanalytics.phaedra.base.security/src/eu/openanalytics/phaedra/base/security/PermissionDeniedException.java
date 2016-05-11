package eu.openanalytics.phaedra.base.security;

public class PermissionDeniedException extends RuntimeException {

	private static final long serialVersionUID = -577597795713579103L;

	public PermissionDeniedException(String message) {
		super(message);
	}

	public PermissionDeniedException(String message, Throwable cause) {
		super(message, cause);
	}
}
