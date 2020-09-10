package eu.openanalytics.phaedra.api.client.impl;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import eu.openanalytics.phaedra.api.client.APIClient;
import eu.openanalytics.phaedra.api.client.APIException;
import eu.openanalytics.phaedra.api.client.model.SessionToken;

public class HTTPAPIClient implements APIClient {

	private static CloseableHttpClient httpClient = HttpClientBuilder.create()
			.setDefaultRequestConfig(RequestConfig.custom()
					.setConnectTimeout(30 * 1000)
					.build())
			.build();

	private String baseURL;
	private String apiPrefix;

	public HTTPAPIClient() {
		apiPrefix = "/api";
	}
	
	@Override
	public SessionToken login(String url, String username, String password) throws APIException {
		this.baseURL = url;
		String token = invokePost(String.format("/login?user=%s&password=%s", username, password), res -> EntityUtils.toString(res.getEntity()));
		return new SessionToken(url, token);
	}
	
	@Override
	public void logout(SessionToken token) throws APIException {
		invokePost("/logout", null);
	}

	private <T> T invokePost(String operation, ResponseProcessor<T> responseProcessor) {
		String targetURL = createURL(operation);
		HttpPost post = new HttpPost(targetURL);
		try (CloseableHttpResponse res = httpClient.execute(post)) {
			if (responseProcessor != null) return responseProcessor.process(res);
		} catch (Exception e) {
			throw new APIException("Failed to invoke API at " + targetURL, e);
		}
		return null;
	}
	
	private String createURL(String operation) {
		return baseURL + apiPrefix + operation;
	}
	
	private static interface ResponseProcessor<T> {
		public T process(CloseableHttpResponse response) throws Exception;
	}
}
