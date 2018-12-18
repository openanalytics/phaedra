package eu.openanalytics.phaedra.api.client;

import eu.openanalytics.phaedra.api.client.impl.HTTPAPIClient;

public class APIClientFactory {

	public static APIClient createDefault() {
		return new HTTPAPIClient();
	}
}
