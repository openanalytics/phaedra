package eu.openanalytics.phaedra.datacapture.config;

public class CaptureConfig {

	private String id;
	private ParameterGroup parameters;
	private ModuleConfig[] moduleConfigs;
	
	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public ParameterGroup getParameters() {
		return parameters;
	}
	
	public void setParameters(ParameterGroup parameters) {
		this.parameters = parameters;
	}
	
	public ModuleConfig[] getModuleConfigs() {
		return moduleConfigs;
	}
	
	public void setModuleConfigs(ModuleConfig[] moduleConfigs) {
		this.moduleConfigs = moduleConfigs;
	}
		
}
