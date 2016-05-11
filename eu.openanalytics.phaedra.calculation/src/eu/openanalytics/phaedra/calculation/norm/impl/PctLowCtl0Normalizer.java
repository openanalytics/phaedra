package eu.openanalytics.phaedra.calculation.norm.impl;


public class PctLowCtl0Normalizer extends PctLowCtlNormalizer {

	@Override
	public String getId() {
		return "%CTL[L=0]";
	}

	@Override
	public String getDescription() {
		return "value = (rawValue - lowMedian / lowMedian) * 100";
	}
}