package eu.openanalytics.phaedra.base.datatype.format;

import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.chrono.ChronoLocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Date;
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
	
	
	private final NumberFormat defaultRealFormat;
	
	private final ConcentrationFormat defaultConcentrationFormat;
	
	private final Map<String, ConcentrationFormat> typeConcentrationFormats;
	
	private final DateTimeFormatter defaultTimestampFormat;
	
	
	public DataFormatter(final NumberFormat realFormat,
			final ConcentrationFormat concentrationFormat,
			final Map<String, ConcentrationFormat> typeConcentrationFormats,
			final DateTimeFormatter timestampFormat) {
		this.defaultRealFormat = realFormat;
		this.defaultConcentrationFormat = concentrationFormat;
		this.typeConcentrationFormats = (typeConcentrationFormats != null) ? typeConcentrationFormats : Collections.emptyMap();
		this.defaultTimestampFormat = timestampFormat;
	}
	
	public DataFormatter(final ConcentrationFormat concentrationFormat,
			final Map<String, ConcentrationFormat> typeConcentrationFormats,
			final DateTimeFormatter timestampFormat) {
		this(null, concentrationFormat, typeConcentrationFormats, timestampFormat);
	}
	
	
	public NumberFormat getRealFormat(final DataDescription dataDescription) {
		return this.defaultRealFormat;
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
	
	
	public DateTimeFormatter getTimestampFormat(final DataDescription dataDescription) {
		return this.defaultTimestampFormat;
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
				{	final NumberFormat numberFormat = getRealFormat(dataDescription);
					if (numberFormat != null) {
						return numberFormat.format(v);
					}
				}
				return Double.toString(v);
			}
		case DateTime: {
				if (dataDescription.getContentType() == ContentType.Timestamp) {
					final DateTimeFormatter format = getTimestampFormat(dataDescription);
					return format.format(toTimestamp(data));
				}
				return data.toString();
			}
		default:
			return data.toString();
		}
	}
	
	private LocalDateTime toTimestamp(Object data) {
		if (data instanceof ChronoLocalDateTime) {
			return LocalDateTime.from((ChronoLocalDateTime)data);
		}
		if (data instanceof Number) {
			data = Instant.ofEpochMilli(((Number)data).longValue());
		}
		else if (data instanceof Date) {
			data = ((Date)data).toInstant();
		}
		if (data instanceof Instant) {
			return LocalDateTime.ofInstant((Instant)data, ZoneId.systemDefault());
		}
		throw new IllegalArgumentException("class= " + data.getClass());
	}
	
}
