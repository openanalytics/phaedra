package eu.openanalytics.phaedra.calculation.norm.impl;

import eu.openanalytics.phaedra.calculation.norm.NormalizationKey;
import eu.openanalytics.phaedra.calculation.norm.NormalizationUtils;

public class ZScoreSamplesNCNormalizer extends BaseNormalizer {
	
	private static int CENTER = 0;
	private static int SCALE = 1;
	
	@Override
	public String getId() {
		return "ZScore[S/NC]";
	}
	
	@Override
	protected double[] calculateControls(NormalizationKey key) {
		double mean = NormalizationUtils.getSamplesLowStat("mean", key);
		double sd = NormalizationUtils.getSamplesLowStat("stdev", key);
		return new double[] { mean, sd };
	}
	
	@Override
	protected double normalizeValue(double rawValue, double[] controls) {
		return (rawValue - controls[CENTER]) / controls[SCALE];
	}
	
}
