package eu.openanalytics.phaedra.calculation.norm.impl;

import eu.openanalytics.phaedra.calculation.norm.NormalizationKey;
import eu.openanalytics.phaedra.calculation.norm.NormalizationUtils;


/**
 * ((highMedian - rawValue) / highMedian) * 100
 */
public class PctHighCtl0Normalizer extends BaseNormalizer {

	@Override
	public String getId() {
		return "%CTL[H=0]";
	}

	@Override
	public String getDescription() {
		//TODO: Update "high control" label to config property value
		return "Robust percent of high control with 0% = HC, 100% = 0";
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