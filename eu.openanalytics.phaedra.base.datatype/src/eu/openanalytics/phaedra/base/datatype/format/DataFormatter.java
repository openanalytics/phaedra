package eu.openanalytics.phaedra.base.datatype.format;

import eu.openanalytics.phaedra.base.datatype.description.ContentType;
import eu.openanalytics.phaedra.base.datatype.description.DataDescription;
import eu.openanalytics.phaedra.base.datatype.unit.DataUnitConfig;


/**
 * Configuration for formatting.
 * 
 * The formatter is immutable and thread-safe.
 */
public class DataFormatter extends DataUnitConfig {
	
	
	public static final int ADD_UNIT = 1 << 0;
	
	
	private final ConcentrationFormat concentrationFormat;
	private volatile ConcentrationFormat concentrationEditFormat;
	
	
	public DataFormatter(final ConcentrationFormat concentrationFormat) {
		super(concentrationFormat.getUnit());
		
		this.concentrationFormat = concentrationFormat;
	}
	
	
	public final ConcentrationFormat getConcentrationFormat() {
		return this.concentrationFormat;
	}
	
	public final ConcentrationFormat getConcentrationEditFormat() {
		ConcentrationFormat format = this.concentrationEditFormat;
		if (format == null) {
			format = createConcentrationEditFormat();
			this.concentrationEditFormat = format;
		}
		return format;
	}
	
	protected ConcentrationFormat createConcentrationEditFormat() {
		return new ConcentrationFormat(getConcentrationUnit(), this.concentrationFormat.getDecimals() + 4);
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
					return getConcentrationFormat().format(v, getConcentrationUnit());
				}
				return Double.toString(v);
			}
		default:
			return data.toString();
		}
	}
	
}
