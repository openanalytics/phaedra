package eu.openanalytics.phaedra.base.datatype.unit;

import org.eclipse.core.databinding.conversion.IConverter;


public class ConcentrationCensorConverter implements IConverter {
	
	
	private final ConcentrationUnit from;
	private final ConcentrationUnit to;
	
	
	public ConcentrationCensorConverter(final ConcentrationUnit from, final ConcentrationUnit to) {
		this.from = from;
		this.to = to;
	}
	
	
	@Override
	public Object getFromType() {
		return String.class;
	}
	
	@Override
	public Object getToType() {
		return String.class;
	}
	
	@Override
	public Object convert(final Object fromObject) {
		if (fromObject == null) {
			return null;
		}
		return this.to.convertCensor((String) fromObject, this.from);
	}
	
}
