package eu.openanalytics.phaedra.base.datatype.description;

import eu.openanalytics.phaedra.base.datatype.DataType;


public class BooleanValueDescription extends BasicDataDescription {
	
	
	public BooleanValueDescription(final String name, final Class<?> entityType) {
		super(name, entityType);
	}
	
	
	@Override
	public final DataType getDataType() {
		return DataType.Boolean;
	}
	
	@Override
	public ContentType getContentType() {
		return ContentType.Other;
	}
	
}
