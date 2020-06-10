package eu.openanalytics.phaedra.base.datatype.description;

import eu.openanalytics.phaedra.base.datatype.DataType;


public class TimestampDescription extends BasicDataDescription {
	
	
	public TimestampDescription(final String name, final Class<?> entityType) {
		super(name, entityType);
	}
	
	
	@Override
	public final DataType getDataType() {
		return DataType.DateTime;
	}
	
	@Override
	public ContentType getContentType() {
		return ContentType.Timestamp;
	}
	
}
