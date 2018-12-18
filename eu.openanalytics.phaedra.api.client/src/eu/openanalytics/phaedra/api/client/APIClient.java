package eu.openanalytics.phaedra.api.client;

import eu.openanalytics.phaedra.api.client.model.SessionToken;

public interface APIClient {

	public SessionToken login(String url, String username, String password) throws APIException;
	
	public void logout(SessionToken token) throws APIException;
}
