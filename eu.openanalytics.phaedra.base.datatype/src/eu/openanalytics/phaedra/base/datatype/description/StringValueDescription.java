package eu.openanalytics.phaedra.base.datatype.description;

import eu.openanalytics.phaedra.base.datatype.DataType;


public class StringValueDescription extends BaseDataDescription {
	
	
	public StringValueDescription(final String name) {
		super(name);
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
