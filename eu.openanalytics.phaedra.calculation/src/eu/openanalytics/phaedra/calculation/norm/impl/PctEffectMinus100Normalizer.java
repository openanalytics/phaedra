package eu.openanalytics.phaedra.calculation.norm.impl;


public class PctEffectMinus100Normalizer extends PctEffectNormalizer {

	@Override
	public String getId() {
		return "%EFFECT (x-100)";
	}

	@Override
	public String getDescription() {
		return "value = (rawValue - lowMedian) / (highMedian - lowMedian) * 100 - 100";
	}

	@Override
	protected double normalizeValue(double rawValue, double[] controls) {
		return super.normalizeValue(rawValue, controls) - 100;
	}
}
