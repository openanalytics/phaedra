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

/**
 * API to interact (read-only) with a PerkinElmer Columbus server instance.
 */
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
	
	/**
	 * Get a list of configured instances.
	 * See {@link Prefs#getLoginIds()}.
	 * 
	 * @return A list of configured instances.
	 */
	public String[] getInstanceIds() {
		return Prefs.getLoginIds();
	}
	
	/**
	 * Get a Columbus instance by its configured ID.
	 * 
	 * @param id The ID of the instance to retrieve.
	 * @return The instance, or null if no match was found.
	 */
	public ColumbusLogin getInstance(String id) {
		return Prefs.load(id);
	}
	
	/**
	 * Get the Columbus instance that is configured as the default instance.
	 * 
	 * @return The default instance, or null if no default is specified.
	 */
	public ColumbusLogin getDefaultInstance() {
		return Prefs.getDefaultLogin();
	}
	
	/**
	 * Connect to a Columbus instance.
	 * <p>
	 * The connection parameters should include at least:
	 * <ul>
	 * <li>InstanceHost</li>
	 * <li>InstancePort</li>
	 * <li>InstanceUsername</li>
	 * <li>InstancePassword</li>
	 * </ul>
	 * </p>
	 * 
	 * @param params A map of connection parameters.
	 * @return An active connection object.
	 */
	public ColumbusWSClient connect(Map<String, Object> params) {
		if (params == null) return connect(null, null, null, null);
		String host = (String) params.get(PARAM_INSTANCE_HOST);
		Integer port = (Integer) params.get(PARAM_INSTANCE_PORT);
		String username = (String) params.get(PARAM_INSTANCE_USERNAME);
		String password = (String) params.get(PARAM_INSTANCE_PASSWORD);
		return connect(host, port, username, password);
	}
	
	/**
	 * Connect to a Columbus instance.
	 * 
	 * @param login The login configuration containing all required connection parameters.
	 * @return An active connection object.
	 */
	public ColumbusWSClient connect(ColumbusLogin login) {
		return connect(login.id);
	}
	
	/**
	 * Connect to a Columbus instance.
	 * 
	 * @param instanceId The ID of a Columbus instance that has been configured in the preferences.
	 * @return An active connection object.
	 */
	public ColumbusWSClient connect(String instanceId) {
		ColumbusLogin login = Prefs.load(instanceId);
		return connect(login.host, login.port, login.username, login.password);
	}

	/**
	 * Connect to a Columbus instance.
	 * 
	 * @param host The hostname of the instance.
	 * @param port The port number of the instance.
	 * @param username The username to log in with.
	 * @param password The password to log in with.
	 * @return An active connection object.
	 */
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

	/**
	 * Get a list of users defined in the Columbus instance.
	 * 
	 * @param client An active Columbus connection.
	 * @return A list of users.
	 * @throws IOException If the query fails for any reason.
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
	
	/**
	 * Get a list of screens defined in the Columbus instance.
	 * 
	 * @param client An active Columbus connection.
	 * @param user The user whose screens should be retrieved.
	 * @return A list of screens.
	 * @throws IOException If the query fails for any reason.
	 */
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
	
	/**
	 * Get a list of plates for a given screen.
	 * 
	 * @param client An active Columbus connection.
	 * @param screenId The ID of a screen whose plates should be retrieved.
	 * @return A list of plates.
	 * @throws IOException If the query fails for any reason.
	 */
	public List<Plate> getPlates(ColumbusWSClient client, long screenId) throws IOException {
		return client.executeList(new GetPlates(screenId));
	}
	
	/**
	 * Get a list of measurements for a given screen and plate.
	 * 
	 * @param client An active Columbus connection.
	 * @param screenId The ID of the screen whose measurements should be retrieved.
	 * @param plateId The ID of the plate whose measurements should be retrieved.
	 * @return A list of measurements.
	 * @throws IOException If the query fails for any reason.
	 */
	public List<Measurement> getMeasurements(ColumbusWSClient client, long screenId, long plateId) throws IOException {
		return client.executeList(new GetMeasurements(plateId, screenId));
	}

	/**
	 * Get the most recent analysis result set for a given measurement.
	 * 
	 * @param client An active Columbus connection.
	 * @param measId The ID of a measurement whose analysis result sets should be inspected.
	 * @return The latest result set, possibly null.
	 * @throws IOException If the query fails for any reason.
	 */
	public Result getLatestResult(ColumbusWSClient client, long measId) throws IOException {
		GetResults getResults = new GetResults(measId);
		client.execute(getResults);
		return getResults.getList().stream().sorted((r1,r2) -> r2.resultDate.compareTo(r1.resultDate)).findFirst().orElse(null);
	}
	
	/**
	 * Get a unique String representation of an analysis resultset.
	 * This String will be unique across all configured Columbus instances.
	 * 
	 * @param client An active Columbus connection.
	 * @param resultId The ID of the result set to generate a unique String for.
	 * @return A unique result set ID.
	 */
	public String getUniqueResultId(ColumbusWSClient client, long resultId) {
		return "result" + resultId + "@" + client.getHost() + ":" + client.getPort();	
	}
	
	/**
	 * Get the contents of an analysis result set.
	 * 
	 * @param client An active Columbus connection.
	 * @param resultId The ID of a result set to get data for.
	 * @return The result data, usually in XML format.
	 * @throws IOException If the query fails for any reason.
	 */
	public String getResultData(ColumbusWSClient client, long resultId) throws IOException {
		GetResult call = new GetResult(resultId);
		client.execute(call);
		return call.getResultValue();
	}
	
	/**
	 * Get a list of wells for the given measurement.
	 * 
	 * @param client An active Columbus connection.
	 * @param measId The ID of the measurement to get wells for.
	 * @return A list of wells.
	 * @throws IOException If the query fails for any reason.
	 */
	public List<Well> getWells(ColumbusWSClient client, long measId) throws IOException {
		return client.executeList(new GetWells(measId));
	}
	
	/**
	 * Get a list of fields for the given measurement and well.
	 * 
	 * @param client An active Columbus connection.
	 * @param wellId The ID of the well to get fields for.
	 * @param measId The ID of the measurement to get fields for.
	 * @return A list of fields.
	 * @throws IOException If the query fails for any reason.
	 */
	public List<Field> getFields(ColumbusWSClient client, long wellId, long measId) throws IOException {
		return client.executeList(new GetFields(wellId, measId));
	}
	
	/**
	 * Get information (dimensions, channels, etc) about the given image.
	 * 
	 * @param client An active Columbus connection.
	 * @param imageId The ID of the image to get info for.
	 * @return Information about the image.
	 * @throws IOException If the query fails for any reason.
	 */
	public ImageInfo getImageInfo(ColumbusWSClient client, long imageId) throws IOException {
		GetImageInfo call = new GetImageInfo(imageId);
		client.execute(call);
		return call.getImageInfo();
	}
	
	/**
	 * Download an image.
	 * 
	 * @param client An active Columbus connection.
	 * @param imageId The ID of the image to download.
	 * @param out The OutputStream to write the image bytes into.
	 * @throws IOException If the query fails for any reason.
	 */
	public void getImage(ColumbusWSClient client, long imageId, OutputStream out) throws IOException {
		GetImage call = new GetImage(imageId, out);
		client.execute(call);
	}
	
	/**
	 * See {@link ColumbusService#setInstanceConfig(Map, ColumbusLogin)}
	 * 
	 * @param params The map to fill.
	 * @param instanceId The ID of the login configuration.
	 */
	public void setInstanceConfig(Map<String, Object> params, String instanceId) {
		setInstanceConfig(params, Prefs.load(instanceId));
	}
	
	/**
	 * Fill a map with connection parameters from the given login configuration.
	 * 
	 * @param params The map to fill.
	 * @param login The login configuration containing connection parameters.
	 */
	public void setInstanceConfig(Map<String, Object> params, ColumbusLogin login) {
		params.put(PARAM_INSTANCE_HOST, login.host);
		params.put(PARAM_INSTANCE_PORT, login.port);
		params.put(PARAM_INSTANCE_USERNAME, login.username);
		params.put(PARAM_INSTANCE_FILE_SHARE, login.fileShare);
		if (login.password != null) params.put(PARAM_INSTANCE_PASSWORD, login.password);
	}
	
	/**
	 * Save a set of resultset IDs into a parameter map.
	 * This can be used to limit the number of resultsets to download during an import job.
	 * 
	 * @param params The parameter map to add the IDs into.
	 * @param resultIds The set of resultset IDs to add.
	 */
	public void setResultIds(Map<String, Object> params, Map<Long, ?> resultIds) {
		Map<Long,Long> value = new HashMap<>();
		for (Long measId: resultIds.keySet()) {
			Object o = resultIds.get(measId);
			if (o instanceof Result) value.put(measId, ((Result)o).resultId);
			else if (o instanceof Long) value.put(measId, ((Long)o));
		}
		params.put(PARAM_RESULT_IDS, value);
	}
	
	/**
	 * Get a set of resultset IDs from a parameter map.
	 * This can be used to limit the number of resultsets to download during an import job.
	 * 
	 * @param params The parameter map.
	 * @return The set of resultset IDs, or null if no IDs were added.
	 */
	@SuppressWarnings("unchecked")
	public Map<Long, Long> getResultIds(Map<String, Object> params) {
		return (Map<Long, Long>) params.get(PARAM_RESULT_IDS);
	}
}
