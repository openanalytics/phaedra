package eu.openanalytics.phaedra.base.datatype.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.chrono.ChronoLocalDateTime;
import java.util.Date;

import eu.openanalytics.phaedra.base.datatype.description.ContentType;
import eu.openanalytics.phaedra.base.datatype.description.DataDescription;
import eu.openanalytics.phaedra.base.datatype.format.DataFormatter;


public class DataValueConverter {
	
	
	private final Object invalidValue;
	
	
	public DataValueConverter(final Object invalidValue) {
		this.invalidValue = invalidValue;
	}
	
	
	/**
	 * Converts value to an object type supported by {@link DataFormatter} for the specified data
	 * description.
	 */
	public Object convert(final DataDescription dataDescription, final Object value) {
		if (value == null) {
			return null;
		}
		switch (dataDescription.getDataType()) {
		case Boolean:
			return checkBoolean(value);
		case Integer:
			return checkInteger(value);
		case Real:
			return checkReal(value);
		case DateTime:
			if (dataDescription.getContentType() == ContentType.Timestamp) {
				return checkTimestamp(value);
			}
			break;
		default:
			break;
		}
		return value;
	}
	
	/**
	 * Converts value to an object type supported by {@link DataFormatter} for the specified data
	 * description.
	 */
	public Object convertArray(final DataDescription dataDescription, final Object value) {
		if (value == null) {
			return null;
		}
		switch (dataDescription.getDataType()) {
		case Boolean:
			if (value instanceof boolean[] || value instanceof Boolean[]) {
				return value;
			}
			if (value instanceof Object[]) {
				final Object[] array = (Object[])value;
				final Object[] convertedArray = convertedArray(array);
				for (int i = 0; i < array.length; i++) {
					convertedArray[i] = checkBoolean(array[i]);
				}
				return convertedArray;
			}
			return this.invalidValue;
		case Integer:
			if (value instanceof long[] || value instanceof Long[]
					|| value instanceof int[] || value instanceof Integer[]) {
				return value;
			}
			if (value instanceof Object[]) {
				final Object[] array = (Object[])value;
				final Object[] convertedArray = convertedArray(array);
				for (int i = 0; i < array.length; i++) {
					convertedArray[i] = checkInteger(array[i]);
				}
				return convertedArray;
			}
			return this.invalidValue;
		case Real:
			if (value instanceof double[] || value instanceof float[]
					|| value instanceof long[] || value instanceof int[]
					|| value instanceof Number[]) {
				return value;
			}
			if (value instanceof Object[]) {
				final Object[] array = (Object[])value;
				final Object[] convertedArray = convertedArray(array);
				for (int i = 0; i < array.length; i++) {
					convertedArray[i] = checkReal(array[i]);
				}
				return convertedArray;
			}
			return this.invalidValue;
		case DateTime:
			if (value instanceof ChronoLocalDateTime[]
					|| value instanceof Instant[]
					|| value instanceof long[] || value instanceof Long[]
					|| value instanceof double[] || value instanceof Double[]
					|| value instanceof float[] || value instanceof Float[]
					|| value instanceof Date[]) {
				return value;
			}
			if (dataDescription.getContentType() == ContentType.Timestamp) {
				final Object[] array = (Object[])value;
				final Object[] convertedArray = convertedArray(array);
				for (int i = 0; i < array.length; i++) {
					convertedArray[i] = checkTimestamp(array[i]);
				}
				return convertedArray;
			}
			break;
		default:
			break;
		}
		return value;
	}
	
	protected Object[] convertedArray(final Object[] array) {
		if (array.getClass().getComponentType() != Object.class) {
			return new Object[array.length];
		}
		return array;
	}
	
	protected Object checkBoolean(final Object value) {
		if (value instanceof Boolean) {
			return value;
		}
		if (value instanceof String) {
			final String s = (String)value;
			if (s.isEmpty()) {
				return null;
			}
			if (s.equalsIgnoreCase("true") || s.equalsIgnoreCase("yes")) {
				return Boolean.TRUE;
			}
			if (s.equalsIgnoreCase("false") || s.equalsIgnoreCase("no")) {
				return Boolean.FALSE;
			}
		}
		return this.invalidValue;
	}
	
	protected Object checkInteger(final Object value) {
		if (value instanceof Long || value instanceof Integer) {
			return value;
		}
		if (value instanceof Number) {
			return Long.valueOf(((Number)value).longValue());
		}
		if (value instanceof String) {
			final String s = (String)value;
			if (s.isEmpty() || s.equals("NA") || s.equals("<NA>")) {
				return null;
			}
			try {
				return Long.decode(s);
			} catch (final Exception e) {}
		}
		return this.invalidValue;
	}
	
	protected Object checkReal(final Object value) {
		if (value instanceof Number) {
			return value;
		}
		if (value instanceof String) {
			final String s = (String)value;
			if (s.isEmpty() || s.equals("NA") || s.equals("<NA>")) {
				return null;
			}
			try {
				return Double.valueOf(s);
			} catch (final Exception e) {}
		}
		return this.invalidValue;
	}
	
	protected Object checkTimestamp(final Object value) {
		if (value instanceof ChronoLocalDateTime
				|| value instanceof Instant
				|| value instanceof Long
				|| value instanceof Double
				|| value instanceof Float
				|| value instanceof Date) {
			return value;
		}
		if (value instanceof String) {
			final String s = (String)value;
			if (s.isEmpty() || s.equals("NA") || s.equals("<NA>")) {
				return null;
			}
			try {
				return LocalDateTime.parse(s);
			} catch (final Exception e) {}
			try {
				return Long.valueOf(s);
			} catch (final Exception e) {}
		}
		return this.invalidValue;
	}
	
}
