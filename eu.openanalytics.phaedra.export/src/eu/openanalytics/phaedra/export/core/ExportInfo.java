package eu.openanalytics.phaedra.export.core;

import java.util.Collections;
import java.util.List;

import eu.openanalytics.phaedra.base.datatype.description.DataDescription;


/**
 * Entry for additional export information.
 */
public class ExportInfo {
	
	
	private final String name;
	private final DataDescription dataDescription;
	private final List<?> values;
	
	
	public ExportInfo(final DataDescription dataDescription, final List<?> values) {
		this.name= dataDescription.getName();
		this.dataDescription = dataDescription;
		this.values = values;
	}
	
	public ExportInfo(final DataDescription dataDescription, final Object value) {
		this.name= dataDescription.getName();
		this.dataDescription = dataDescription;
		this.values = Collections.singletonList(value);
	}
	
	
	/**
	 * Returns the name of the entry.
	 * @return the name
	 */
	public String getName() {
		return this.name;
	}
	
	/**
	 * Returns the data type of the values.
	 * 
	 * @return the value type.
	 */
	public DataDescription getDataType() {
		return this.dataDescription;
	}
	
	/**
	 * Returns the values of the entry.
	 * @return list containing the valus;
	 */
	public List<?> getValues() {
		return this.values;
	}
	
}
