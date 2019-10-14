package eu.openanalytics.phaedra.base.datatype.description;

import eu.openanalytics.phaedra.base.datatype.DataType;


public class TimestampDescription extends BaseDataDescription {
	
	
	public TimestampDescription(final String name) {
		super(name);
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
