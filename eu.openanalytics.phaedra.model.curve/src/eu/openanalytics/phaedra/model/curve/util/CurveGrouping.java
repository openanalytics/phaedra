package eu.openanalytics.phaedra.model.curve.util;

import java.util.Arrays;

import eu.openanalytics.phaedra.base.util.misc.StringUtils;

/**
 * Dose-response curves can be grouped on specific properties (which are just regular well features).
 * <p>
 * E.g. suppose that a plate contains 10 samples of a compound at various concentrations. 5 samples are infected with a virus
 * and the 5 other samples are not. An additional well feature named 'Virus' represents this infection property.
 * </p>
 * <p>
 * Without grouping, these 10 samples would be fit onto a single dose-response curve for each feature being fit.
 * </p>
 * <p>
 * With grouping by 'Virus', each feature being fit would result in 2 dose-response curves for these 10 samples.
 * </p>
 */
public class CurveGrouping {

	private String[] keys = EMPTY;
	private String[] values = EMPTY;
	
	private static final String[] EMPTY = new String[0];
	
	public CurveGrouping(String[] groupingKeys, String[] groupingValues) {
		if (groupingKeys != null) keys = Arrays.stream(groupingKeys).filter(s -> s != null && !s.isEmpty()).toArray(i -> new String[i]);
		if (groupingValues != null) values = Arrays.stream(groupingValues).filter(s -> s != null && !s.isEmpty()).toArray(i -> new String[i]);
	}
	
	public int getCount() {
		return Math.min(keys.length, values.length);
	}
	
	public String getKey(int nr) {
		return keys[nr];
	}
	
	public String get(int nr) {
		return values[nr];
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(keys);
		result = prime * result + Arrays.hashCode(values);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CurveGrouping other = (CurveGrouping) obj;
		if (!Arrays.equals(keys, other.keys))
			return false;
		if (!Arrays.equals(values, other.values))
			return false;
		return true;
	}
	
	@Override
	public String toString() {
		if (getCount() == 0) return "";
		return StringUtils.createSeparatedString(values, ",");
	}
}
