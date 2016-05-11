package eu.openanalytics.phaedra.calculation.norm.impl;

import eu.openanalytics.phaedra.calculation.norm.NormalizationKey;
import eu.openanalytics.phaedra.calculation.norm.NormalizationUtils;

public class PctLowCtl100Normalizer extends BaseNormalizer {

	@Override
	public String getId() {
		return "%CTL[L=100]";
	}

	@Override
	public String getDescription() {
		return "value = (rawValue / lowMedian) * 100";
	}

	@Override
	protected double[] calculateControls(NormalizationKey key) {
		double median = NormalizationUtils.getLowStat("median", key);
		return new double[] {median};
	}
	
	@Override
	protected double normalizeValue(double rawValue, double[] controls) {
		return (rawValue / controls[0]) * 100;
	}
}