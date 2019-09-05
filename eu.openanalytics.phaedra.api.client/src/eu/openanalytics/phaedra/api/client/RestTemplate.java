package eu.openanalytics.phaedra.api.client;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.function.Function;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import eu.openanalytics.phaedra.base.util.io.StreamUtils;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;

public class RestTemplate implements AutoCloseable {

	private static CloseableHttpClient client;
	private static Gson gson;
	
	private static RestTemplate instance = new RestTemplate();
	
	private RestTemplate() {
		client = HttpClients.createDefault();
		gson = new GsonBuilder().create();
	}
	
	public static RestTemplate getInstance() {
		return instance;
	}
	
	public <T> T getForObject(String url, Class<T> objectClass) throws IOException {
		HttpGet get = new HttpGet(url);
		return executeRequest(get, res -> parseObject(res, objectClass));
	}
	
	public <T> T postForObject(String url, String body, Class<T> objectClass) throws IOException {
		HttpPost post = new HttpPost(url);
		post.setEntity(new StringEntity(body));
		return executeRequest(post, res -> parseObject(res, objectClass));
	}
	
	@Override
	public void close() throws Exception {
		client.close();
	}
	
	private <T> T executeRequest(HttpUriRequest request, Function<CloseableHttpResponse, T> responseConsumer) throws IOException {
		long start = System.currentTimeMillis();
		try (CloseableHttpResponse response = client.execute(request)) {
			int code = response.getStatusLine().getStatusCode();
			long duration = System.currentTimeMillis() - start;
			EclipseLog.debug(String.format("Request executed in %d ms [URI: %s, response: %d]", duration, request.getURI(), code), RestTemplate.class);
			
			switch (code) {
				case HttpStatus.SC_OK:
					return responseConsumer.apply(response);
				case HttpStatus.SC_NOT_FOUND:
					throw new IOException("Requested item not found");
				case HttpStatus.SC_INTERNAL_SERVER_ERROR: 
					byte[] responseBody = StreamUtils.readAll(response.getEntity().getContent());
					throw new IOException("Error while processing request: " + new String(responseBody));
				default:
					throw new IOException("Unexpected response: " + code);
			}
		}
	}
	
	private <T> T parseObject(CloseableHttpResponse response, Class<T> objectClass) {
		try {
			return gson.fromJson(new InputStreamReader(response.getEntity().getContent()), objectClass);
		} catch (IOException e) {
			throw new RuntimeException("Failed to parse response", e);
		}
	}
}
