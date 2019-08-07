package eu.openanalytics.phaedra.calculation.norm.impl;

import eu.openanalytics.phaedra.calculation.norm.NormalizationKey;
import eu.openanalytics.phaedra.calculation.norm.NormalizationUtils;


/**
 * Robust Z-Score based on low controls
 * (rawValue - lowMedian) / (1.4826 * lowMAD)
 */
public class ZScoreRobLowNormalizer extends BaseNormalizer {
	
	private static int CENTER = 0;
	private static int SCALE = 1;
	
	
	@Override
	public String getId() {
		return "ZScoreRob[L]";
	}
	
	@Override
	public String getDescription() {
		return "Robust Z-score based on low/negative controls";
	}
	
	@Override
	protected double[] calculateControls(NormalizationKey key) {
		double median = NormalizationUtils.getLowStat("median", key);
		double mad = NormalizationUtils.getLowStat("mad", key);
		return new double[] { median, mad * 1.4826 };
	}
	
	@Override
	protected double normalizeValue(double rawValue, double[] controls) {
		return (rawValue - controls[CENTER]) / controls[SCALE];
	}
	
}
