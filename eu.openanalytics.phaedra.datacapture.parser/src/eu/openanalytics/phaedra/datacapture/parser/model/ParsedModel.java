package eu.openanalytics.phaedra.datacapture.parser.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An in-memory representation of a parsed data source.
 * A ParsedModel may contain one or more {@link ParsedPlate}s.
 */
public class ParsedModel {

	private List<ParsedPlate> plates;
	private Map<String,Object> properties;
	
	public ParsedModel() {
		plates = new ArrayList<ParsedPlate>();
		properties = new HashMap<String, Object>();
	}
	
	public ParsedPlate[] getPlates() {
		return plates.toArray(new ParsedPlate[plates.size()]);
	}
	
	public ParsedPlate getPlate(int index) {
		if (index >= plates.size()) return null;
		return plates.get(index);
	}
	
	public void addPlate(ParsedPlate plate) {
		plates.add(plate);
	}
	
	public String[] getPropertyNames() {
		return properties.keySet().toArray(new String[properties.size()]);
	}
	
	public Object getProperty(String name) {
		return properties.get(name);
	}
	
	public void addProperty(String name, Object value) {
		properties.put(name, value);
	}
}
