package eu.openanalytics.phaedra.datacapture.config;

import java.util.HashMap;
import java.util.Map;

public class ParameterGroup {
	
	private Map<String,Object> parameters;
	
	public ParameterGroup() {
		parameters = new HashMap<String, Object>();
	}
	
	public String[] getParameterKeys() {
		return parameters.keySet().toArray(new String[parameters.size()]);
	}
	
	public boolean contains(String key) {
		return parameters.containsKey(key);
	}
	
	public void setParameter(String key, Object value) {
		if (key == null || key.isEmpty()) return;
		parameters.put(key, value);
	}
	
	public Object getParameter(String key) {
		if (key == null || key.isEmpty()) return null;
		return parameters.get(key);
	}
}
