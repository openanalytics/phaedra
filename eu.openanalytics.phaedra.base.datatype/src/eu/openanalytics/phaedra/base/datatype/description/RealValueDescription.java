package eu.openanalytics.phaedra.base.datatype.description;

import eu.openanalytics.phaedra.base.datatype.DataType;


public class RealValueDescription extends BaseDataDescription {
	
	
	public RealValueDescription(final String name, final Class<?> entityType) {
		super(name, entityType);
	}
	
	
	@Override
	public final DataType getDataType() {
		return DataType.Real;
	}
	
	@Override
	public ContentType getContentType() {
		return ContentType.Other;
	}
	
}
