package eu.openanalytics.phaedra.model.protocol.util;

import java.text.Format;
import java.util.HashMap;
import java.util.Map;

import eu.openanalytics.phaedra.base.util.misc.NumberUtils;
import eu.openanalytics.phaedra.model.protocol.vo.IFeature;

public class Formatters {

	private static Map<String,Format> formatters = new HashMap<String, Format>();

	private static Formatters instance;

	private Formatters() {
		// Private constructor.
		formatters = new HashMap<String, Format>();
	}

	public static Formatters getInstance() {
		if (instance == null) instance = new Formatters();
		return instance;
	}

	public String format(double value, IFeature feature) {
		if (feature == null) return "" + value;
		String formatString = feature.getFormatString();
		return format(value, formatString);
	}

	public String format(double value, String formatString) {
		if (Double.isNaN(value) || formatString == null || formatString.isEmpty()) return "" + value;
		Format format = getFormatter(formatString);
		return format.format(value);
	}

	public String format(Object value, IFeature feature) {
		if (feature == null || !feature.isNumeric()) return "" + value;
		String formatString = feature.getFormatString();
		return format(value, formatString);
	}

	public String format(Object value, String formatString) {
		if (value instanceof Number) {
			return format(((Number)value).doubleValue(), formatString);
		}

		if (formatString == null || formatString.isEmpty()) return "" + value;
		Format format = getFormatter(formatString);
		return format.format(value);
	}

	private static Format getFormatter(String formatString) {
		Format format = formatters.get(formatString);
		if (format == null) {
			format = NumberUtils.createDecimalFormat(formatString);
			formatters.put(formatString, format);
		}
		return format;
	}

}
