package eu.openanalytics.phaedra.export.core.query;

import java.util.HashMap;
import java.util.Map;

import eu.openanalytics.phaedra.base.datatype.description.DataDescription;


public class Query {
	
	
	public static String checkColumnLabel(String name) {
		return name.toUpperCase().replace(' ', '_');
	}
	
	
	private String sql;
	
	private Map<String, DataDescription> colDataType = new HashMap<>();
	
	
	public void setSql(String sql) {
		this.sql = sql;
	}
	
	public String getSql() {
		return sql;
	}
	
	public void setColumnDataType(String columnLabel, DataDescription type) {
		this.colDataType.put(columnLabel, type);
	}
	
	public /*@Nullable*/ DataDescription getColumnDataType(String columnLabel) {
		return this.colDataType.get(columnLabel);
	}
	
}
