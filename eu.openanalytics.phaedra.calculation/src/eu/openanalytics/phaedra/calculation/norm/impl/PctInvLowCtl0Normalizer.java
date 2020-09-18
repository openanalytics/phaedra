package eu.openanalytics.phaedra.calculation.norm.impl;


/**
 * - (rawValue - lowMedian / lowMedian) * 100
 */
public class PctInvLowCtl0Normalizer extends PctLowCtlNormalizer {

	@Override
	public String getId() {
		return "%CTL[INV L=0]";
	}

	@Override
	public String getDescription() {
		//TODO: Update "Low Control" label to config property value
		return "Robust percent of low control with 0% = LC, 100% = 0";
	}
	
	@Override
	protected double normalizeValue(double rawValue, double[] controls) {
		return -super.normalizeValue(rawValue, controls);
	}
}
