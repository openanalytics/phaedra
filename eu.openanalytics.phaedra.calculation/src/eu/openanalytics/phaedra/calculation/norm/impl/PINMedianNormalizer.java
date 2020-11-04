package eu.openanalytics.phaedra.calculation.norm.impl;

import eu.openanalytics.phaedra.calculation.norm.NormalizationKey;
import eu.openanalytics.phaedra.calculation.norm.NormalizationUtils;

public class PINMedianNormalizer extends BaseNormalizer {

	private static int HIGH_STAT = 0;
	private static int LOW_STAT = 1;

	@Override
	public String getId() {
		return "PIN[Median]";
	}

	@Override
	protected double[] calculateControls(NormalizationKey key) {
		double highStat = NormalizationUtils.getHighStat("median", key);
		double lowStat = NormalizationUtils.getLowStat("median", key);
		return new double[] { highStat, lowStat };
	}

	@Override
	protected double normalizeValue(double rawValue, double[] controls) {
		return (rawValue - controls[LOW_STAT]) / (controls[HIGH_STAT] - controls[LOW_STAT]) * 100;
	}

}
