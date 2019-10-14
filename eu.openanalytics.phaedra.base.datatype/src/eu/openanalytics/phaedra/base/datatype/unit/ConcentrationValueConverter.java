package eu.openanalytics.phaedra.base.datatype.unit;

import static java.util.Objects.requireNonNull;

import org.eclipse.core.databinding.conversion.IConverter;


public class ConcentrationValueConverter implements IConverter {
	
	
	private final ConcentrationUnit from;
	private final ConcentrationUnit to;
	
	
	public ConcentrationValueConverter(final ConcentrationUnit from, final ConcentrationUnit to) {
		this.from = requireNonNull(from);
		this.to = requireNonNull(to);
	}
	
	
	@Override
	public Object getFromType() {
		return Double.class;
	}
	
	@Override
	public Object getToType() {
		return Double.class;
	}
	
	@Override
	public Object convert(final Object fromObject) {
		if (fromObject == null) {
			return null;
		}
		return this.to.convert((double) fromObject, this.from);
	}
	
}
