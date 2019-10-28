package eu.openanalytics.phaedra.base.datatype.format;

import java.util.Collections;
import java.util.Map;

import eu.openanalytics.phaedra.base.datatype.description.ContentType;
import eu.openanalytics.phaedra.base.datatype.description.DataDescription;
import eu.openanalytics.phaedra.base.datatype.description.DataUnitConfig;
import eu.openanalytics.phaedra.base.datatype.unit.ConcentrationUnit;


/**
 * Configuration for formatting.
 * 
 * The formatter is immutable and thread-safe.
 */
public class DataFormatter implements DataUnitConfig {
	
	
//	public static final int ADD_UNIT = 1 << 0;
	
	
	private final ConcentrationFormat defaultConcentrationFormat;
	
	private final Map<String, ConcentrationFormat> typeConcentrationFormats;
	
	
	public DataFormatter(final ConcentrationFormat concentrationFormat,
			final Map<String, ConcentrationFormat> typeConcentrationFormats) {
		this.defaultConcentrationFormat = concentrationFormat;
		this.typeConcentrationFormats = (typeConcentrationFormats != null) ? typeConcentrationFormats : Collections.emptyMap();
	}
	
	
	@Override
	public ConcentrationUnit getConcentrationUnit(final DataDescription dataDescription) {
		return getConcentrationFormat(dataDescription).getUnit();
	}
	
	public ConcentrationFormat getConcentrationFormat(final DataDescription dataDescription) {
		ConcentrationFormat format = null;
		if (dataDescription != null) {
			format = this.typeConcentrationFormats.get(dataDescription.getEntityType().getName());
		}
		return (format != null) ? format : this.defaultConcentrationFormat;
	}
	
	public ConcentrationFormat getConcentrationEditFormat(final DataDescription dataDescription) {
		return createConcentrationEditFormat(getConcentrationFormat(dataDescription));
	}
	
	protected ConcentrationFormat createConcentrationEditFormat(final ConcentrationFormat concentrationFormat) {
		return new ConcentrationFormat(concentrationFormat.getUnit(), concentrationFormat.getDecimals() + 4);
	}
	
	
	public String getNAString(final DataDescription dataDescription) {
		return "";
	}
	
	public String format(Object data, final DataDescription dataDescription) {
		data = dataDescription.convertDataTo(data, this);
		if (data == null) {
			return getNAString(dataDescription);
		}
		switch (dataDescription.getDataType()) {
		case Integer: {
				final long v = ((Number)data).longValue();
				return Long.toString(v);
			}
		case Real: {
				final double v = ((Number)data).doubleValue();
				if (dataDescription.getContentType() == ContentType.Concentration) {
					final ConcentrationFormat format = getConcentrationFormat(dataDescription);
					return format.format(v, format.getUnit());
				}
				return Double.toString(v);
			}
		default:
			return data.toString();
		}
	}
	
}
