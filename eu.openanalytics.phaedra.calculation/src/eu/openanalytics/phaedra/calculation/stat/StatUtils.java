package eu.openanalytics.phaedra.calculation.stat;

import java.math.BigDecimal;
import java.text.DecimalFormat;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;

import eu.openanalytics.phaedra.base.util.misc.NumberUtils;

public class StatUtils {
	
	public final static DecimalFormat DEFAULT_NUMBER_FORMAT = NumberUtils.createDecimalFormat("0.00##");
	
	public static String format(double v) {
		if (canBeFormatted(v)) return DEFAULT_NUMBER_FORMAT.format(v);
		return ""+v;
	}
	
	public static boolean canBeFormatted(double v) {
		if (Double.isNaN(v)) return false;
		if (Double.isInfinite(v)) return false;
		return true;
	}
	
	public static DescriptiveStatistics createStats(double[] values) {
		DescriptiveStatistics stats = new DescriptiveStatistics();
		for (double value: values) stats.addValue(value);
		return stats;
	}

	public static double round(double value, int decimals) {
		if (Double.isNaN(value) || Double.isInfinite(value)) return value;
		BigDecimal bd = new BigDecimal(value);
		bd = bd.setScale(decimals, BigDecimal.ROUND_UP);
		value = bd.doubleValue();
		return value;
	}
	
}
