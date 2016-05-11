package eu.openanalytics.phaedra.calculation.norm.impl;

import eu.openanalytics.phaedra.calculation.norm.NormalizationKey;
import eu.openanalytics.phaedra.calculation.norm.NormalizationUtils;

public class SigmaHighNormalizer extends BaseNormalizer {

	@Override
	public String getId() {
		return "Sigma[H]";
	}

	@Override
	public String getDescription() {
		return "value = (rawValue - highMedian / highStDev)";
	}

	@Override
	protected double[] calculateControls(NormalizationKey key) {
		double median = NormalizationUtils.getHighStat("median", key);
		double stDev = NormalizationUtils.getHighStat("stdev", key);
		return new double[] {median, stDev};
	}

	@Override
	protected double normalizeValue(double rawValue, double[] controls) {
		return ((rawValue - controls[0]) / controls[1]);
	}
}