package eu.openanalytics.phaedra.calculation.norm.impl;

import eu.openanalytics.phaedra.calculation.norm.NormalizationKey;
import eu.openanalytics.phaedra.calculation.norm.NormalizationUtils;


/**
 * (rawValue / highMedian) * 100
 */
public class PctCtlNormalizer extends BaseNormalizer {

	@Override
	public String getId() {
		return "%CTL";
	}

	@Override
	public String getDescription() {
		return "Robust percent of high control with 0% = 0, 100% = HC";
	}

	@Override
	protected double[] calculateControls(NormalizationKey key) {
		double median = NormalizationUtils.getHighStat("median", key);
		return new double[] {median};
	}

	@Override
	protected double normalizeValue(double rawValue, double[] controls) {
		return (rawValue / controls[0]) * 100;
	}
}
