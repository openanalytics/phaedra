package eu.openanalytics.phaedra.base.model;

import eu.openanalytics.phaedra.base.datatype.description.DataDescription;


public abstract class BasicEntityProperty<T> implements EntityProperty<T> {
	
	
	private final String key;
	
	private final String label;
	
	private final DataDescription dataDescription;
	
	
	public BasicEntityProperty(final String key, final String label,
			final DataDescription dataDescription) {
		this.key= key;
		this.label= label;
		this.dataDescription= dataDescription;
	}
	
	
	@Override
	public String getKey() {
		return this.key;
	}
	
	@Override
	public String getLabel() {
		return this.label;
	}
	
	@Override
	public DataDescription getDataDescription() {
		return this.dataDescription;
	}
	
}
