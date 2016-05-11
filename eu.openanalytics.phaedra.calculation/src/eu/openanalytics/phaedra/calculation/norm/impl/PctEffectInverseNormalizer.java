package eu.openanalytics.phaedra.calculation.norm.impl;


public class PctEffectInverseNormalizer extends PctEffectNormalizer {

	@Override
	public String getId() {
		return "%EFFECT (100-x)";
	}

	@Override
	public String getDescription() {
		return "value = 100 - ((rawValue - lowMedian) / (highMedian - lowMedian) * 100)";
	}

	@Override
	protected double normalizeValue(double rawValue, double[] controls) {
		return 100 - super.normalizeValue(rawValue, controls);
	}
}
