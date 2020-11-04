package eu.openanalytics.phaedra.calculation.norm.impl;

import eu.openanalytics.phaedra.calculation.norm.NormalizationKey;
import eu.openanalytics.phaedra.calculation.norm.NormalizationUtils;

public class PctCtlHigh0Normalizer extends BaseNormalizer {

	@Override
	public String getId() {
		return "%CTL[H=0]";
	}

	@Override
	protected double[] calculateControls(NormalizationKey key) {
		double median = NormalizationUtils.getHighStat("median", key);
		return new double[] {median};
	}

	@Override
	protected double normalizeValue(double rawValue, double[] controls) {
		return ((controls[0] - rawValue) / controls[0]) * 100;
	}
}