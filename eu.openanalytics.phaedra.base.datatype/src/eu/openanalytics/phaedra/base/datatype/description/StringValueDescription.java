package eu.openanalytics.phaedra.base.datatype.description;

import eu.openanalytics.phaedra.base.datatype.DataType;


public class StringValueDescription extends BasicDataDescription {
	
	
	public StringValueDescription(final String name, final Class<?> entityType) {
		super(name, entityType);
	}
	
	
	@Override
	public final DataType getDataType() {
		return DataType.String;
	}
	
	@Override
	public ContentType getContentType() {
		return ContentType.Other;
	}
	
}
