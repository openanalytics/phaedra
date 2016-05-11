package eu.openanalytics.phaedra.datacapture;

import java.util.HashMap;
import java.util.Map;

public class DataCaptureTask {

	private String id;
	private String user;
	private String configId;
	private String source;
	private String[] moduleFilter;
	private boolean test;
	private Map<String, Object> parameters;
	
	// Parameters related to data capture:
	public final static String PARAM_NR_THREADS = "nrOfThreads";
	public final static String PARAM_EXPERIMENT_NAME = "experimentName";
	public final static String PARAM_PROTOCOL_NAME = "protocolName";
	public final static String PARAM_TEST = "test";
	// Parameters related to data linking:
	public final static String PARAM_ALLOW_AUTO_LINK = "allowAutoLink";
	public final static String PARAM_CREATE_NEW_EXP = "createNewExperiment";
	public final static String PARAM_CREATE_MISSING_WELL_FEATURES = "createMissingWellFeatures";
	public final static String PARAM_CREATE_MISSING_SUBWELL_FEATURES = "createMissingSubWellFeatures";
	
	public DataCaptureTask() {
		parameters = new HashMap<>();
	}
	
	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public String getUser() {
		return user;
	}
	
	public void setUser(String user) {
		this.user = user;
	}
	
	public String getConfigId() {
		return configId;
	}
	
	public void setConfigId(String configId) {
		this.configId = configId;
	}
	
	public String getSource() {
		return source;
	}
	
	public void setSource(String source) {
		this.source = source;
	}

	public String[] getModuleFilter() {
		return moduleFilter;
	}
	
	public void setModuleFilter(String[] moduleFilter) {
		this.moduleFilter = moduleFilter;
	}
	
	public void setTest(boolean test) {
		this.test = test;
	}
	
	public boolean isTest() {
		return test;
	}
	
	public Map<String, Object> getParameters() {
		return parameters;
	}
}
