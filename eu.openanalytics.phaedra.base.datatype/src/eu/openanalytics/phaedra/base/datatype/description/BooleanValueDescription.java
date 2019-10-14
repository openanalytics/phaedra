package eu.openanalytics.phaedra.base.datatype.description;

import eu.openanalytics.phaedra.base.datatype.DataType;


public class BooleanValueDescription extends BaseDataDescription {
	
	
	public BooleanValueDescription(final String name) {
		super(name);
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
