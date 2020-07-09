package eu.openanalytics.phaedra.base.ui.richtableviewer.column;

import java.util.List;
import java.util.Map;


public class CustomDataUtils {
	
	
	public static void set(final Map<String, Object> customData, final String propertyKey,
			final Object value) {
		if (value != null) {
			customData.put(propertyKey, value);
		}
		else {
			customData.remove(propertyKey);
		}
	}
	
	
	public static Long getLong(final Map<String, Object> customData, final String propertyKey,
			final Long defaultValue) {
		Object obj = (customData != null) ? customData.get(propertyKey) : null;
		if (obj instanceof Long) {
			return (Long)obj;
		}
		Long value = null;
		if (obj instanceof String) {
			try {
				value = Long.parseLong((String)obj);
			} catch (final NumberFormatException e) {}
		}
		if (value == null) {
			value = defaultValue;
		}
		return value;
	}
	
	public static long getLongValue(final Map<String, Object> customData, final String propertyKey,
			final long defaultValue) {
		return getLong(customData, propertyKey, defaultValue);
	}
	
	public static Long checkLong(final Map<String, Object> customData, final String propertyKey,
			final Long defaultValue) {
		Object obj = customData.get(propertyKey);
		if (obj instanceof Long) {
			return (Long)obj;
		}
		Long value = null;
		if (obj instanceof String) {
			try {
				value = Long.parseLong((String)obj);
			} catch (final NumberFormatException e) {}
		}
		if (value == null) {
			value = defaultValue;
		}
		set(customData, propertyKey, value);
		return value;
	}
	
	public static long checkLongValue(final Map<String, Object> customData, final String propertyKey,
			final long defaultValue) {
		return checkLong(customData, propertyKey, defaultValue);
	}
	
	
	public static Double getDouble(final Map<String, Object> customData, final String propertyKey,
			final Double defaultValue) {
		Object obj = (customData != null) ? customData.get(propertyKey) : null;
		if (obj instanceof Double) {
			return (Double)obj;
		}
		Double value = null;
		if (obj instanceof String) {
			try {
				value = Double.parseDouble((String)obj);
			} catch (final NumberFormatException e) {}
		}
		if (value == null) {
			value = defaultValue;
		}
		return value;
	}
	
	public static double getDoubleValue(final Map<String, Object> customData, final String propertyKey,
			final double defaultValue) {
		return getDouble(customData, propertyKey, defaultValue);
	}
	
	public static Double checkDouble(final Map<String, Object> customData, final String propertyKey,
			final Double defaultValue) {
		Object obj = customData.get(propertyKey);
		if (obj instanceof Double) {
			return (Double)obj;
		}
		Double value = null;
		if (obj instanceof String) {
			try {
				value = Double.parseDouble((String)obj);
			} catch (final NumberFormatException e) {}
		}
		if (value == null) {
			value = defaultValue;
		}
		set(customData, propertyKey, value);
		return value;
	}
	
	public static double checkDoubleValue(final Map<String, Object> customData, final String propertyKey,
			final double defaultValue) {
		return checkDouble(customData, propertyKey, defaultValue);
	}
	
	
	public static String getString(final Map<String, Object> customData, final String propertyKey,
			final String defaultValue) {
		Object obj = (customData != null) ? customData.get(propertyKey) : null;
		if (obj instanceof String) {
			return (String)obj;
		}
		return defaultValue;
	}
	
	public static String checkString(final Map<String, Object> customData, final String propertyKey,
			final String defaultValue) {
		Object obj = customData.get(propertyKey);
		if (obj instanceof String) {
			return (String)obj;
		}
		String value = defaultValue;
		set(customData, propertyKey, value);
		return value;
	}
	
	
	public static <T extends OptionType<?, ?, ?>> T getType(final Map<String, Object> customData, final String propertyKey,
			final List<? extends T> availableTypes, final T defaultType) {
		if (availableTypes.isEmpty()) {
			return null;
		}
		Object obj = customData.get(propertyKey);
		if (availableTypes.get(0).getOptionType().isInstance(obj)) {
			return (T)obj;
		}
		T type = null;
		if (obj instanceof String) {
			type = OptionType.getType(availableTypes, (String)obj);
		}
		if (type == null) {
			type = defaultType;
		}
		return type;
	}
	
	public static <T extends OptionType<?, ?, ?>> T checkType(final Map<String, Object> customData, final String propertyKey,
			final List<? extends T> availableTypes, final T defaultType) {
		if (availableTypes.isEmpty()) {
			return null;
		}
		Object obj = customData.get(propertyKey);
		if (availableTypes.get(0).getOptionType().isInstance(obj)) {
			return (T)obj;
		}
		T type = null;
		if (obj instanceof String) {
			type = OptionType.getType(availableTypes, (String)obj);
		}
		if (type == null) {
			type = defaultType;
		}
		set(customData, propertyKey, type);
		return type;
	}
	
	public static <T extends OptionType<?, ?, ?>> T checkTypeDefaultFirst(final Map<String, Object> customData, final String propertyKey,
			final List<? extends T> availableTypes) {
		return checkType(customData, propertyKey, availableTypes, availableTypes.get(0));
	}
	
}
