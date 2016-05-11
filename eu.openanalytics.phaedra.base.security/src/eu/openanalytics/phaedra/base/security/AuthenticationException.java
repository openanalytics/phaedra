package eu.openanalytics.phaedra.base.security;

public class AuthenticationException extends RuntimeException {

	private static final long serialVersionUID = 5243072626126051104L;

	public AuthenticationException(String message) {
		super(message);
	}

	public AuthenticationException(String message, Throwable cause) {
		super(message, cause);
	}
}
