package eu.openanalytics.phaedra.calculation.stat.impl;

import eu.openanalytics.phaedra.calculation.stat.ctx.IStatContext;

public class SumCalculator extends BaseStatCalculator {

	@Override
	public double calculate(IStatContext context) {
		double sum = 0;
		double[] values = context.getData(0);
		for (double v: values) {
			if (!Double.isNaN(v)) {
				sum += v;
			}
		}
		return sum;
	}

}