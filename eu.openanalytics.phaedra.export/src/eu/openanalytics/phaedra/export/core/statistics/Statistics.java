package eu.openanalytics.phaedra.export.core.statistics;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Statistics {

	private Map<String, Double> stats;
	
	public Statistics() {
		stats = new HashMap<String, Double>();
	}
	
	public Set<String> getStatNames() {
		return stats.keySet();
	}

	public void set(String statName, double value) {
		stats.put(statName, value);
	}
	
	public double get(String statName) {
		Double v = stats.get(statName);
		if (v != null) return v;
		return Double.NaN;
	}
}
