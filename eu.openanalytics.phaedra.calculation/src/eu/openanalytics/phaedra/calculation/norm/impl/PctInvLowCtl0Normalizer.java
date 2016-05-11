package eu.openanalytics.phaedra.calculation.norm.impl;


public class PctInvLowCtl0Normalizer extends PctLowCtlNormalizer {

	@Override
	public String getId() {
		return "%CTL[INV L=0]";
	}

	@Override
	public String getDescription() {
		return "value = - (rawValue - lowMedian / lowMedian) * 100";
	}
	
	@Override
	protected double normalizeValue(double rawValue, double[] controls) {
		return -super.normalizeValue(rawValue, controls);
	}
}
