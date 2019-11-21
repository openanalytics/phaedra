package eu.openanalytics.phaedra.base.security;

import java.util.Collection;


public interface ILoginHandler {
	
	
	String USERNAME = "username";
	String PASSWORD = "password";
	
	
	/**
	 * Returns a set of required parameters (input) for login.
	 * 
	 * @return a collection of parameter ids
	 */
	Collection<String> getRequiredParameter();
	
	/**
	 * Attempt to authenticate the the user identity. Username and password is provided
	 * if {@link #getRequiredParameter() required}.
	 * If the authentication succeeds, this method configures the security service and
	 * simply returns.
	 * Otherwise it will throw an AuthenticationException.
	 * 
	 * @param userName The unique username of the user who wants to log in, if required
	 * @param password The password associated with the provided username, if required
	 * @param setUserContext True to create a security context for the newly authenticated user. False to simply attempt authentication for the given user.
	 * @throws AuthenticationException If authentication goes wrong for any reason.
	 */
	public void authenticate(String userName, byte[] password, boolean setUserContext) throws AuthenticationException;

}
