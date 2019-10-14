package eu.openanalytics.phaedra.base.datatype.format;

import static java.util.Objects.requireNonNull;

import org.eclipse.core.databinding.conversion.IConverter;

import eu.openanalytics.phaedra.base.datatype.unit.ConcentrationUnit;


/**
 * Converts a concentration value (Double) to a formatted string.
 */
public class ConcentrationValueFormatConverter implements IConverter {
	
	
	private final ConcentrationUnit from;
	private final ConcentrationFormat format;
	
	
	public ConcentrationValueFormatConverter(final ConcentrationUnit from, final ConcentrationFormat format) {
		this.from = requireNonNull(from);
		this.format = requireNonNull(format);
	}
	
	
	@Override
	public Object getFromType() {
		return Double.class;
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
		return this.format.format((double) fromObject, this.from);
	}
	
}
