package eu.openanalytics.phaedra.base.datatype.description;

import eu.openanalytics.phaedra.base.datatype.DataType;


public class ByteArrayDescription extends BasicDataDescription {
	
	
	public ByteArrayDescription(final String name, final Class<?> entityType) {
		super(name, entityType);
	}
	
	
	@Override
	public final DataType getDataType() {
		return DataType.ByteArray;
	}
	
	@Override
	public ContentType getContentType() {
		return ContentType.Other;
	}
	
}
