package eu.openanalytics.phaedra.calculation.norm.impl;

import eu.openanalytics.phaedra.calculation.norm.NormalizationKey;
import eu.openanalytics.phaedra.calculation.norm.NormalizationUtils;


/**
 * Z-Score based on low controls
 * (rawValue - lowMean) / lowSD
 */
public class ZScoreLowNormalizer extends BaseNormalizer {
	
	private static int CENTER = 0;
	private static int SCALE = 1;
	
	
	@Override
	public String getId() {
		return "ZScore[L]";
	}
	
	@Override
	public String getDescription() {
		return "Z-score based on low/negative controls";
	}
	
	@Override
	protected double[] calculateControls(NormalizationKey key) {
		double mean = NormalizationUtils.getLowStat("mean", key);
		double sd = NormalizationUtils.getLowStat("stdev", key);
		return new double[] { mean, sd };
	}
	
	@Override
	protected double normalizeValue(double rawValue, double[] controls) {
		return (rawValue - controls[CENTER]) / controls[SCALE];
	}
	
}
