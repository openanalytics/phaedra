package eu.openanalytics.phaedra.datacapture.columbus.ws;

import java.io.IOException;
import java.util.List;

import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;

import eu.openanalytics.phaedra.base.environment.Screening;
import eu.openanalytics.phaedra.datacapture.columbus.ws.operation.BaseListOperation;
import eu.openanalytics.phaedra.datacapture.columbus.ws.operation.BaseOperation;

public class ColumbusWSClient implements AutoCloseable {
	
	private HttpClient client;
	private ConnectionInfo connectionInfo;
	private MultiThreadedHttpConnectionManager connectionManager;
	
	public void initialize(String host, int port, String username, String password) {
		connectionManager = new MultiThreadedHttpConnectionManager();
		connectionManager.getParams().setMaxConnectionsPerHost(HostConfiguration.ANY_HOST_CONFIGURATION, 20);
		this.client = new HttpClient(connectionManager);
		this.connectionInfo = new ConnectionInfo();
		this.connectionInfo.host = host;
		this.connectionInfo.port = port;
		this.connectionInfo.username = username;
		this.connectionInfo.password = password;
		this.connectionInfo.endpoint = "http://" + host + ":" + port + "/axis2/services/CCInterface2";
		
		// If no password is provided, try to obtain one from the password store.
		if (password == null || password.isEmpty()) {
			try {
				String pwKey = username + "@" + host;
				this.connectionInfo.password = Screening.getEnvironment().getConfig().resolvePassword(pwKey);
			} catch (Exception e) {
				throw new IllegalArgumentException("No password found for Columbus user " + username);
			}
		}
	}
	
	public void execute(BaseOperation operation) throws IOException {
		operation.execute(client, connectionInfo);
	}
	
	public <T> List<T> executeList(BaseListOperation<T> operation) throws IOException {
		operation.execute(client, connectionInfo);
		return operation.getList();
	}
	
	public String getHost() {
		return connectionInfo.host;
	}
	
	public int getPort() {
		return connectionInfo.port;
	}
	
	public void close() {
		connectionManager.shutdown();
	}
	
	public static class ConnectionInfo {
		public String host;
		public int port;
		public String username;
		public String password;
		public String endpoint;
	}
}
