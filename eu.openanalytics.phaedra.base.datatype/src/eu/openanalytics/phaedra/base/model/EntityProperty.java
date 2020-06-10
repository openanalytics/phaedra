package eu.openanalytics.phaedra.base.model;

import eu.openanalytics.phaedra.base.datatype.description.DataDescription;


public interface EntityProperty<T> {
	
	
	/**
	 * Returns the unique key (no spaces) of the property.
	 * @return the key
	 */
	String getKey();
	
	/**
	 * Returns the label of the property.
	 * @return the label
	 */
	String getLabel();
	/**
	 * Returns a short label of the property, e.g. for column headers in the UI.
	 * @return the label
	 */
	String getShortLabel();
	
	DataDescription getDataDescription();
	
	Object getTypedValue(T vo);
	
}
