package eu.openanalytics.phaedra.base.datatype.description;

import eu.openanalytics.phaedra.base.datatype.DataType;


public class IntegerValueDescription extends BaseDataDescription {
	
	
	public IntegerValueDescription(final String name) {
		super(name);
	}
	
	
	@Override
	public final DataType getDataType() {
		return DataType.Integer;
	}
	
	@Override
	public ContentType getContentType() {
		return ContentType.Other;
	}
	
}
