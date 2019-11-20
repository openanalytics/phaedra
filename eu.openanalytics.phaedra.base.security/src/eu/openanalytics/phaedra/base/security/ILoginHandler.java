package eu.openanalytics.phaedra.base.security;

public interface ILoginHandler {

	/**
	 * Attempt to authenticate the provided username and password.
	 * If the authentication succeeds, this method simply returns.
	 * If not, it will throw an AuthenticationException.
	 * 
	 * @param userName The unique username of the user who wants to log in.
	 * @param password The password associated with the provided username.
	 * @param setUserContext True to create a security context for the newly authenticated user. False to simply attempt authentication for the given user.
	 * @throws AuthenticationException If authentication goes wrong for any reason.
	 */
	public void authenticate(String userName, byte[] password, boolean setUserContext) throws AuthenticationException;

}
