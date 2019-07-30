package eu.openanalytics.phaedra.calculation.norm.impl;

import eu.openanalytics.phaedra.calculation.norm.NormalizationKey;
import eu.openanalytics.phaedra.calculation.norm.NormalizationUtils;

/** Robust Z-Score based on all samples */
public class ZScoreRobNormalizer extends BaseNormalizer {
	
	private static int CENTER = 0;
	private static int SCALE = 1;
	
	
	@Override
	public String getId() {
		return "ZScore[Rob]";
	}
	
	@Override
	public String getDescription() {
		return "value = (rawValue - median) / (1.4826 * mad)";
	}
	
	@Override
	protected double[] calculateControls(NormalizationKey key) {
		double median = NormalizationUtils.getAllStat("median", key);
		double mad = NormalizationUtils.getAllStat("mad", key);
		return new double[] { median, mad * 1.4826 };
	}
	
	@Override
	protected double normalizeValue(double rawValue, double[] controls) {
		return (rawValue - controls[CENTER]) / controls[SCALE];
	}
	
}
