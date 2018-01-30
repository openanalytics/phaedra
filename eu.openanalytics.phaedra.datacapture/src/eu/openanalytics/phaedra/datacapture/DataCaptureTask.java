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
	
	public enum DataCaptureParameter {
		TargetProtocol,			// To capture into a new experiment
		TargetExperimentName,
		
		TargetExperiment,		// To capture into an existing experiment

		PlateMapping,			// To capture into existing plates
		
		CreateMissingWellFeatures,
		CreateMissingSubWellFeatures
	};
	
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
	
	public boolean isTest() {
		return test;
	}
	
	public void setTest(boolean test) {
		this.test = test;
	}
	
	public Map<String, Object> getParameters() {
		return parameters;
	}
}
