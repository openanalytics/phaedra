package eu.openanalytics.phaedra.calculation.norm.impl;

import eu.openanalytics.phaedra.calculation.norm.NormalizationKey;
import eu.openanalytics.phaedra.calculation.norm.NormalizationUtils;

public class PctCtlNormalizer extends BaseNormalizer {

	@Override
	public String getId() {
		return "%CTL";
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
