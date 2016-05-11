package eu.openanalytics.phaedra.datacapture.columbus;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.openanalytics.phaedra.datacapture.columbus.prefs.Prefs;
import eu.openanalytics.phaedra.datacapture.columbus.prefs.Prefs.ColumbusLogin;
import eu.openanalytics.phaedra.datacapture.columbus.ws.ColumbusWSClient;
import eu.openanalytics.phaedra.datacapture.columbus.ws.operation.GetFields;
import eu.openanalytics.phaedra.datacapture.columbus.ws.operation.GetFields.Field;
import eu.openanalytics.phaedra.datacapture.columbus.ws.operation.GetImage;
import eu.openanalytics.phaedra.datacapture.columbus.ws.operation.GetImageInfo;
import eu.openanalytics.phaedra.datacapture.columbus.ws.operation.GetImageInfo.ImageInfo;
import eu.openanalytics.phaedra.datacapture.columbus.ws.operation.GetMeasurements;
import eu.openanalytics.phaedra.datacapture.columbus.ws.operation.GetMeasurements.Measurement;
import eu.openanalytics.phaedra.datacapture.columbus.ws.operation.GetPlates;
import eu.openanalytics.phaedra.datacapture.columbus.ws.operation.GetPlates.Plate;
import eu.openanalytics.phaedra.datacapture.columbus.ws.operation.GetResult;
import eu.openanalytics.phaedra.datacapture.columbus.ws.operation.GetResults;
import eu.openanalytics.phaedra.datacapture.columbus.ws.operation.GetResults.Result;
import eu.openanalytics.phaedra.datacapture.columbus.ws.operation.GetScreens;
import eu.openanalytics.phaedra.datacapture.columbus.ws.operation.GetScreens.Screen;
import eu.openanalytics.phaedra.datacapture.columbus.ws.operation.GetUserGroups;
import eu.openanalytics.phaedra.datacapture.columbus.ws.operation.GetUserGroups.UserGroup;
import eu.openanalytics.phaedra.datacapture.columbus.ws.operation.GetUsers;
import eu.openanalytics.phaedra.datacapture.columbus.ws.operation.GetUsers.User;
import eu.openanalytics.phaedra.datacapture.columbus.ws.operation.GetWells;
import eu.openanalytics.phaedra.datacapture.columbus.ws.operation.GetWells.Well;

public class ColumbusService {

	private static final String PARAM_INSTANCE_HOST = "InstanceHost";
	private static final String PARAM_INSTANCE_PORT = "InstancePort";
	private static final String PARAM_INSTANCE_FILE_SHARE = "InstanceFileShare";
	private static final String PARAM_INSTANCE_USERNAME = "InstanceUsername";
	private static final String PARAM_INSTANCE_PASSWORD = "InstancePassword";
	
	private static final String PARAM_RESULT_IDS = "Results";
	
	private static ColumbusService instance = new ColumbusService();
	
	private ColumbusService() {
		// Hidden constructor
	}
	
	public static ColumbusService getInstance() {
		return instance;
	}
	
	/*
	 * Instance API
	 * ************
	 */
	
	public String[] getInstanceIds() {
		return Prefs.getLoginIds();
	}
	
	public ColumbusLogin getInstance(String id) {
		return Prefs.load(id);
	}
	
	public ColumbusLogin getDefaultInstance() {
		return Prefs.getDefaultLogin();
	}
	
	/*
	 * Connection API
	 * **************
	 */
	
	public ColumbusWSClient connect(Map<String, Object> params) {
		if (params == null) return connect(null, null, null, null);
		String host = (String) params.get(PARAM_INSTANCE_HOST);
		Integer port = (Integer) params.get(PARAM_INSTANCE_PORT);
		String username = (String) params.get(PARAM_INSTANCE_USERNAME);
		String password = (String) params.get(PARAM_INSTANCE_PASSWORD);
		return connect(host, port, username, password);
	}
	
	public ColumbusWSClient connect(ColumbusLogin login) {
		return connect(login.id);
	}
	
	public ColumbusWSClient connect(String instanceId) {
		ColumbusLogin login = Prefs.load(instanceId);
		return connect(login.host, login.port, login.username, login.password);
	}
	
	public ColumbusWSClient connect(String host, Integer port, String username, String password) {
		ColumbusLogin defaultLogin = Prefs.getDefaultLogin();
		
		if (host == null || host.isEmpty()) host = defaultLogin.host;
		if (port == null || port == 0) port = defaultLogin.port;
		if (username == null || username.isEmpty()) username = defaultLogin.username;
		
		if (host == null || host.isEmpty()) throw new IllegalArgumentException("Missing required parameter: " + PARAM_INSTANCE_HOST);
		if (username == null || username.isEmpty()) throw new IllegalArgumentException("Missing required parameter: " + PARAM_INSTANCE_USERNAME);
		// Note: password may be empty, in which case an attempt will be made to look it up in the server's password store.
		
		ColumbusWSClient client = new ColumbusWSClient();
		client.initialize(host, port, username, password);
		return client;
	}

	/*
	 * Screen-Plate-Well API
	 * *********************
	 */

	public List<User> getUsers(ColumbusWSClient client) throws IOException {
		Map<String, User> uniqueUsers = new HashMap<>();
		GetUserGroups getUserGroups = new GetUserGroups();
		client.execute(getUserGroups);
		for (UserGroup group: getUserGroups.getList()) {
			GetUsers getUsers = new GetUsers(group.groupId);
			client.execute(getUsers);
			for (User user: getUsers.getList()) {
				uniqueUsers.put(user.loginname, user);
			}
		}
		return new ArrayList<>(uniqueUsers.values());
	}
	
	public List<Screen> getScreens(ColumbusWSClient client, String user) throws IOException {
		GetUserGroups getUserGroups = new GetUserGroups();
		client.execute(getUserGroups);
		
		for (UserGroup group: getUserGroups.getList()) {
			GetUsers getUsers = new GetUsers(group.groupId);
			client.execute(getUsers);
			
			for (User u: getUsers.getList()) {
				if (user.equalsIgnoreCase(u.loginname)) {
					GetScreens getScreens = new GetScreens(u.userId);
					client.execute(getScreens);
					return getScreens.getList();
				}
			}
		}
		
		return Collections.emptyList();
	}
	
	public List<Plate> getPlates(ColumbusWSClient client, long screenId) throws IOException {
		return client.executeList(new GetPlates(screenId));
	}
	
	public List<Measurement> getMeasurements(ColumbusWSClient client, long screenId, long plateId) throws IOException {
		return client.executeList(new GetMeasurements(plateId, screenId));
	}
	
	public Result getLatestResult(ColumbusWSClient client, long measId) throws IOException {
		GetResults getResults = new GetResults(measId);
		client.execute(getResults);
		return getResults.getList().stream().sorted((r1,r2) -> r2.resultDate.compareTo(r1.resultDate)).findFirst().orElse(null);
	}
	
	public String getUniqueResultId(ColumbusWSClient client, long resultId) {
		return "result" + resultId + "@" + client.getHost() + ":" + client.getPort();	
	}
	
	public String getResultData(ColumbusWSClient client, long resultId) throws IOException {
		GetResult call = new GetResult(resultId);
		client.execute(call);
		return call.getResultValue();
	}
	
	public List<Well> getWells(ColumbusWSClient client, long measId) throws IOException {
		return client.executeList(new GetWells(measId));
	}
	
	public List<Field> getFields(ColumbusWSClient client, long wellId, long measId) throws IOException {
		return client.executeList(new GetFields(wellId, measId));
	}
	
	public ImageInfo getImageInfo(ColumbusWSClient client, long imageId) throws IOException {
		GetImageInfo call = new GetImageInfo(imageId);
		client.execute(call);
		return call.getImageInfo();
	}
	
	public void getImage(ColumbusWSClient client, long imageId, OutputStream out) throws IOException {
		GetImage call = new GetImage(imageId, out);
		client.execute(call);
	}
	
	/*
	 * DataCapture parameter helpers
	 * *****************************
	 */
	
	public void setInstanceConfig(Map<String, Object> params, String instanceId) {
		setInstanceConfig(params, Prefs.load(instanceId));
	}
	
	public void setInstanceConfig(Map<String, Object> params, ColumbusLogin login) {
		params.put(PARAM_INSTANCE_HOST, login.host);
		params.put(PARAM_INSTANCE_PORT, login.port);
		params.put(PARAM_INSTANCE_USERNAME, login.username);
		params.put(PARAM_INSTANCE_FILE_SHARE, login.fileShare);
		if (login.password != null) params.put(PARAM_INSTANCE_PASSWORD, login.password);
	}
	
	public void setResultIds(Map<String, Object> params, Map<Long, ?> resultIds) {
		Map<Long,Long> value = new HashMap<>();
		for (Long measId: resultIds.keySet()) {
			Object o = resultIds.get(measId);
			if (o instanceof Result) value.put(measId, ((Result)o).resultId);
			else if (o instanceof Long) value.put(measId, ((Long)o));
		}
		params.put(PARAM_RESULT_IDS, value);
	}
	
	@SuppressWarnings("unchecked")
	public Map<Long, Long> getResultIds(Map<String, Object> params) {
		return (Map<Long, Long>) params.get(PARAM_RESULT_IDS);
	}
}
