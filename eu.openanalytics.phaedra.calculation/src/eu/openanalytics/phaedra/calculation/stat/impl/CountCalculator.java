package eu.openanalytics.phaedra.calculation.stat.impl;

import eu.openanalytics.phaedra.calculation.stat.ctx.IStatContext;

public class CountCalculator extends BaseStatCalculator {

	@Override
	public double calculate(IStatContext context) {
		double[] values = context.getData(0);
		return values.length;
	}

}