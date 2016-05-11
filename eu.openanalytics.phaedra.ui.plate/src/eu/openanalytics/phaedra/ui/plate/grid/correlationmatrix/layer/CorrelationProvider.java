package eu.openanalytics.phaedra.ui.plate.grid.correlationmatrix.layer;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.commons.math3.stat.correlation.SpearmansCorrelation;
import org.apache.commons.math3.stat.ranking.NaNStrategy;
import org.apache.commons.math3.stat.ranking.NaturalRanking;

public class CorrelationProvider {

	private static final String PEARSONS = "P";
	private static final String SPEARMANS = "S";

	private static Map<String, Object> cache = new HashMap<>();

	public static double getValue(CorrelationType type, RealMatrix matrix, int col, int row) {
		if (matrix == null) return Double.NaN;

		switch (type) {
		case PEARSONS: {
			String key = PEARSONS + System.identityHashCode(matrix);
			PearsonsCorrelation cor;
			if (cache.containsKey(key)) {
				cor = (PearsonsCorrelation) cache.get(key);
			} else {
				cor = new PearsonsCorrelation(matrix);
				cache.put(key, cor);
			}
			return cor.getCorrelationMatrix().getEntry(row, col);
		}
		case PEARSONS_STANDARD_ERRORS: {
			String key = PEARSONS + System.identityHashCode(matrix);
			PearsonsCorrelation cor;
			if (cache.containsKey(key)) {
				cor = (PearsonsCorrelation) cache.get(key);
			} else {
				cor = new PearsonsCorrelation(matrix);
				cache.put(key, cor);
			}
			return cor.getCorrelationStandardErrors().getEntry(row, col);
		}
		case SPEARMANS:
			String key = SPEARMANS + System.identityHashCode(matrix);
			SpearmansCorrelation cor;
			if (cache.containsKey(key)) {
				cor = (SpearmansCorrelation) cache.get(key);
			} else {
				NaturalRanking ranking = new NaturalRanking(NaNStrategy.REMOVED);
				cor = new SpearmansCorrelation(matrix, ranking);
				cache.put(key, cor);
			}
			return cor.getCorrelationMatrix().getEntry(row, col);
		default:
			return Double.NaN;
		}
	}

	public enum CorrelationType {
		PEARSONS("Pearsons Correlation")
		, PEARSONS_STANDARD_ERRORS("Pearsons Correlation Standard Errors")
		, SPEARMANS("Spearmans Correlation")
		;

		private String name;

		private CorrelationType(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

	}

}
