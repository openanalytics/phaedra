package eu.openanalytics.phaedra.api.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.Header;
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

//TODO support other POST body content types besides json
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
		return executeRequestForObject(get, objectClass);
	}
	
	public String getForString(String url) throws IOException {
		HttpGet get = new HttpGet(url);
		return executeRequestForString(get);
	}
	
	public void get(String url, ResponseConsumer responseConsumer) throws IOException {
		HttpGet get = new HttpGet(url);
		executeRequest(get, responseConsumer);
	}
	
	public <T> T postForObject(String url, String body, Class<T> objectClass) throws IOException {
		HttpPost post = new HttpPost(url);
		post.setEntity(new StringEntity(body));
		post.addHeader("Content-Type", "application/json");
		return executeRequestForObject(post, objectClass);
	}
	
	public String postForString(String url, String body) throws IOException {
		HttpPost post = new HttpPost(url);
		post.setEntity(new StringEntity(body));
		post.addHeader("Content-Type", "application/json");
		return executeRequestForString(post);
	}
	
	public void post(String url, String body, ResponseConsumer responseConsumer) throws IOException {
		HttpPost post = new HttpPost(url);
		post.setEntity(new StringEntity(body));
		post.addHeader("Content-Type", "application/json");
		executeRequest(post, responseConsumer);
	}
	
	@Override
	public void close() throws Exception {
		client.close();
	}
	
	private <T> T executeRequestForObject(HttpUriRequest request, Class<T> objectClass) throws IOException {
		Pair<String, T> returnValue = new MutablePair<>("returnValue", null);
		executeRequest(request, (statusCode, headers, responseBody) -> {
			returnValue.setValue(gson.fromJson(new InputStreamReader(responseBody, StandardCharsets.UTF_8), objectClass));
		});
		return returnValue.getValue();
	}
	
	private String executeRequestForString(HttpUriRequest request) throws IOException {
		Pair<String, String> returnValue = new MutablePair<>("returnValue", null);
		executeRequest(request, (statusCode, headers, responseBody) -> {
			byte[] bytes = StreamUtils.readAll(responseBody);
			returnValue.setValue(new String(bytes, StandardCharsets.UTF_8));
		});
		return returnValue.getValue();
	}
	
	private void executeRequest(HttpUriRequest request, ResponseConsumer responseConsumer) throws IOException {
		long start = System.currentTimeMillis();
		try (CloseableHttpResponse response = client.execute(request)) {
			int code = response.getStatusLine().getStatusCode();
			long duration = System.currentTimeMillis() - start;
			EclipseLog.debug(String.format("Request executed in %d ms [URI: %s, response: %d]", duration, request.getURI(), code), RestTemplate.class);
			
			switch (code) {
				case HttpStatus.SC_OK:
					if (responseConsumer != null) {
						Map<String,String> headers = new HashMap<>();
						for (Header header: response.getAllHeaders()) {
							headers.put(header.getName(), header.getValue());
						}
						InputStream body = response.getEntity() == null ? null : response.getEntity().getContent();
						responseConsumer.consume(
								code,
								headers,
								body);
					}
					break;
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
	
	public interface ResponseConsumer {
		public void consume(int statusCode, Map<String,String> headers, InputStream body) throws IOException;
	}
}
