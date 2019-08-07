package eu.openanalytics.phaedra.calculation.norm.impl;

import eu.openanalytics.phaedra.calculation.norm.NormalizationKey;
import eu.openanalytics.phaedra.calculation.norm.NormalizationUtils;


/**
 * Robust Z-Score based on samples
 * (rawValue - samplesMedian) / (1.4826 * samplesMAD)
 */
public class ZScoreRobSamplesNormalizer extends BaseNormalizer {
	
	private static int CENTER = 0;
	private static int SCALE = 1;
	
	
	@Override
	public String getId() {
		return "ZScoreRob[S]";
	}
	
	@Override
	public String getDescription() {
		return "Robust Z-score based on samples";
	}
	
	@Override
	protected double[] calculateControls(NormalizationKey key) {
		double median = NormalizationUtils.getSamplesStat("median", key);
		double mad = NormalizationUtils.getSamplesStat("mad", key);
		return new double[] { median, mad * 1.4826 };
	}
	
	@Override
	protected double normalizeValue(double rawValue, double[] controls) {
		return (rawValue - controls[CENTER]) / controls[SCALE];
	}
	
}
