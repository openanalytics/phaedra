package eu.openanalytics.phaedra.calculation.norm.impl;

import eu.openanalytics.phaedra.calculation.norm.NormalizationKey;
import eu.openanalytics.phaedra.calculation.norm.NormalizationUtils;

/** Negative Percent Inhibition based on mean of controls */
public class PctInhNegMeanNormalizer extends BaseNormalizer {

	private static int HIGH_STAT = 0;
	private static int LOW_STAT = 1;


	@Override
	public String getId() {
		return "PIN[Neg Mean]"; // alt %INH %INHIBITION
	}

	@Override
	public String getDescription() {
		return "value = 100 - (rawValue - lowMean) / (highMean - lowMean) * 100";
	}

	@Override
	protected double[] calculateControls(NormalizationKey key) {
		double highStat = NormalizationUtils.getHighStat("mean", key);
		double lowStat = NormalizationUtils.getLowStat("mean", key);
		return new double[] { highStat, lowStat };
	}

	@Override
	protected double normalizeValue(double rawValue, double[] controls) {
		return 100 - (rawValue - controls[LOW_STAT]) / (controls[HIGH_STAT] - controls[LOW_STAT]) * 100;
	}

}
