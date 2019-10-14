package eu.openanalytics.phaedra.base.datatype.description;

import eu.openanalytics.phaedra.base.datatype.DataType;


public class ByteArrayDescription extends BaseDataDescription {
	
	
	public ByteArrayDescription(final String name) {
		super(name);
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
