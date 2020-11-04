package eu.openanalytics.phaedra.calculation.norm.impl;

import eu.openanalytics.phaedra.calculation.norm.NormalizationKey;
import eu.openanalytics.phaedra.calculation.norm.NormalizationUtils;

public class SigmaLowNormalizer extends BaseNormalizer {

	@Override
	public String getId() {
		return "Sigma[L]";
	}

	@Override
	protected double[] calculateControls(NormalizationKey key) {
		double median = NormalizationUtils.getLowStat("median", key);
		double stDev = NormalizationUtils.getLowStat("stdev", key);
		return new double[] {median, stDev};
	}

	@Override
	protected double normalizeValue(double rawValue, double[] controls) {
		return ((rawValue - controls[0]) / controls[1]);
	}
}