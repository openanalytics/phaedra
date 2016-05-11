package eu.openanalytics.phaedra.calculation.norm.impl;


public class MinusPctEffectNormalizer extends PctEffectNormalizer {

	@Override
	public String getId() {
		return "%EFFECT (-x)";
	}

	@Override
	public String getDescription() {
		return "value = (lowMedian - rawValue) / (highMedian - lowMedian) * 100";
	}

	@Override
	protected double normalizeValue(double rawValue, double[] controls) {
		return -super.normalizeValue(rawValue, controls);
	}
}
