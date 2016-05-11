package eu.openanalytics.phaedra.model.plate.compound;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CompoundInfo {

	public static final String SMILES = "Smiles";
	public static final String SALTFORM = "Saltform";
	
	private Map<String, String> values;
		
	public CompoundInfo() {
		values = new HashMap<>();
	}
	
	public List<String> getKeys() {
		List<String> keys = new ArrayList<>(values.keySet());
		keys.sort(null);
		return keys;
	}
	
	public void set(String key, String value) {
		values.put(key, value);
	}
	
	public String get(String key) {
		return values.get(key);
	}
	
	public String getSmiles() {
		for (String key: values.keySet()) {
			if (key.equalsIgnoreCase(SMILES)) return get(key);
		}
		return null;
	}
	
	public void setSmiles(String smiles) {
		set(SMILES, smiles);
	}
	
	public String getSaltform() {
		for (String key: values.keySet()) {
			if (key.equalsIgnoreCase(SALTFORM)) return get(key);
		}
		return null;
	}
	
	public void setSaltform(String saltform) {
		set(SALTFORM, saltform);
	}
}
