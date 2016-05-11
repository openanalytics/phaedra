package eu.openanalytics.phaedra.datacapture.parser.model;

import java.util.HashMap;
import java.util.Map;

public class ParsedPlate {

	private Map<String,ParsedWell> wells;
	private Map<String,Object> properties;
	
	private int rows;
	private int columns;
	
	public ParsedPlate() {
		wells = new HashMap<String, ParsedWell>();
		properties = new HashMap<String, Object>();
	}
	
	public ParsedWell getWell(int row, int col) {
		String key = ""+row+"#"+col;
		return wells.get(key);
	}
	
	public ParsedWell[] getWells() {
		return wells.values().toArray(new ParsedWell[wells.size()]);
	}
	
	public void addWell(int row, int col, ParsedWell well) {
		String key = ""+row+"#"+col;
		wells.put(key, well);
		well.setRow(row);
		well.setColumn(col);
	}
	
	public void removeWell(int row, int col) {
		String key = ""+row+"#"+col;
		wells.remove(key);		
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

	public int getRows() {
		return rows;
	}

	public void setRows(int rows) {
		this.rows = rows;
	}

	public int getColumns() {
		return columns;
	}

	public void setColumns(int columns) {
		this.columns = columns;
	}
}
