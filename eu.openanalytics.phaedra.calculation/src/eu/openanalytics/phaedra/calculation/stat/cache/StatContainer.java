package eu.openanalytics.phaedra.calculation.stat.cache;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class StatContainer implements Serializable {

	private static final long serialVersionUID = 2890273962041328713L;

	private Map<String, Double> stats;

	public StatContainer() {
		stats = new HashMap<String, Double>();
	}

	public double get(String stat) {
		Double value = stats.get(stat);
		if (value == null) return Double.NaN;
		return value;
	}

	public boolean contains(String stat) {
		return stats.containsKey(stat);
	}

	public void add(String stat, double value) {
		stats.put(stat, value);
	}

	public void copyTo(StatContainer other) {
		if (other == null) return;
		for (String stat: stats.keySet()) {
			double v = get(stat);
			double otherV = other.get(stat);
			// Only replace NaNs with not-NaNs.
			if (Double.isNaN(otherV) && !Double.isNaN(v)) other.add(stat, v);
		}
	}
}
