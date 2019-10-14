package eu.openanalytics.phaedra.base.datatype.format;

import java.text.Format;
import java.util.HashMap;
import java.util.Map;

import eu.openanalytics.phaedra.base.util.misc.NumberUtils;


/**
 * Formatting utility.
 */
public class Formatter {
	
	private static final ThreadLocal<Formatter> INSTANCES = ThreadLocal.withInitial(Formatter::new);
	
	
	/**
	 * Returns a formatter for the current thread.
	 */
	public static Formatter getInstance() {
		return INSTANCES.get();
	}
	
	
	private final Map<String, Format> formatters = new HashMap<>();
	
	
	private Formatter() {
	}
	
	
	public String format(final double value, final String formatString) {
		if (Double.isNaN(value) || formatString == null || formatString.isEmpty()) {
			return "" + value;
		}
		final Format format = getFormatter(formatString);
		return format.format(value);
	}
	
	public String format(final Object value, final String formatString) {
		if (value instanceof Number) {
			return format(((Number)value).doubleValue(), formatString);
		}
		
		if (formatString == null || formatString.isEmpty()) {
			return "" + value;
		}
		final Format format = getFormatter(formatString);
		return format.format(value);
	}
	
	
	private Format getFormatter(final String formatString) {
		Format format = this.formatters.get(formatString);
		if (format == null) {
			format = NumberUtils.createDecimalFormat(formatString);
			this.formatters.put(formatString, format);
		}
		return format;
	}
	
}
