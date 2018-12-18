package eu.openanalytics.phaedra.api.client;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import eu.openanalytics.phaedra.api.client.model.SessionToken;

public class APIClientSessionManager {

	private Map<String, SessionToken> activeSessions;
	
	public APIClientSessionManager() {
		activeSessions = new ConcurrentHashMap<>();
	}
	
	public void register(String username, SessionToken token) {
		activeSessions.put(username, token);
	}
	
	public SessionToken getToken(String username) {
		return activeSessions.get(username);
	}
}
