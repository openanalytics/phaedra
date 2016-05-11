package eu.openanalytics.phaedra.base.seda;

import java.util.HashMap;
import java.util.Map;

public class StageConfiguration {

	private Map<String, Object> config;
	
	public StageConfiguration() {
		config = new HashMap<>();
	}
	
	public void set(String name, Object value) {
		config.put(name, value);
	}
	
	public Object get(String name) {
		return config.get(name);
	}
	
	public int getInt(String name) {
		Object o = get(name);
		if (o instanceof Number) {
			return ((Number)o).intValue();
		} else if (o instanceof String) {
			return Integer.parseInt((String)o);
		}
		return 0;
	}
	
	public String getString(String name) {
		Object o = get(name);
		if (o != null) return o.toString();
		return null;
	}
}
