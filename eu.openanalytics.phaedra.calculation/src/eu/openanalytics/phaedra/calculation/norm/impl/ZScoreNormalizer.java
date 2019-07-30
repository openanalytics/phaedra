package eu.openanalytics.phaedra.calculation.norm.impl;

import eu.openanalytics.phaedra.calculation.norm.NormalizationKey;
import eu.openanalytics.phaedra.calculation.norm.NormalizationUtils;

/** Z-Score based on all samples (common Z-Score) */
public class ZScoreNormalizer extends BaseNormalizer {
	
	private static int CENTER = 0;
	private static int SCALE = 1;
	
	
	@Override
	public String getId() {
		return "ZScore";
	}
	
	@Override
	public String getDescription() {
		return "value = (rawValue - mean) / sd";
	}
	
	@Override
	protected double[] calculateControls(NormalizationKey key) {
		double mean = NormalizationUtils.getAllStat("mean", key);
		double sd = NormalizationUtils.getAllStat("stdev", key);
		return new double[] { mean, sd };
	}
	
	@Override
	protected double normalizeValue(double rawValue, double[] controls) {
		return (rawValue - controls[CENTER]) / controls[SCALE];
	}
	
}
