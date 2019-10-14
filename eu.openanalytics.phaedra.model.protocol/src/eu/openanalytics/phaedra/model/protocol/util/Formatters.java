package eu.openanalytics.phaedra.model.protocol.util;

import eu.openanalytics.phaedra.base.datatype.format.Formatter;
import eu.openanalytics.phaedra.model.protocol.vo.IFeature;


public class Formatters {
	
	private static final ThreadLocal<Formatters> INSTANCES = ThreadLocal.withInitial(Formatters::new);
	
	public static Formatters getInstance() {
		return INSTANCES.get();
	}
	
	
	private Formatters() {
	}

	public String format(double value, IFeature feature) {
		if (feature == null) return "" + value;
		String formatString = feature.getFormatString();
		return format(value, formatString);
	}

	public String format(double value, String formatString) {
		return Formatter.getInstance().format(value, formatString);
	}

	public String format(Object value, IFeature feature) {
		if (feature == null || !feature.isNumeric()) return "" + value;
		String formatString = feature.getFormatString();
		return Formatter.getInstance().format(value, formatString);
	}

}
