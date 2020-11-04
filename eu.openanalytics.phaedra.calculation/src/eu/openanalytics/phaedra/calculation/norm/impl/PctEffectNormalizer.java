package eu.openanalytics.phaedra.calculation.norm.impl;

import eu.openanalytics.phaedra.calculation.norm.NormalizationKey;
import eu.openanalytics.phaedra.calculation.norm.NormalizationUtils;

public class PctEffectNormalizer extends BaseNormalizer {

	@Override
	public String getId() {
		return "%EFFECT";
	}

	@Override
	protected double[] calculateControls(NormalizationKey key) {
		double highMedian = NormalizationUtils.getHighStat("median", key);
		double lowMedian = NormalizationUtils.getLowStat("median", key);
		return new double[] {highMedian, lowMedian};
	}
	
	@Override
	protected double normalizeValue(double rawValue, double[] controls) {
		return (rawValue - controls[1]) / (controls[0] - controls[1]) * 100;
	}
}
