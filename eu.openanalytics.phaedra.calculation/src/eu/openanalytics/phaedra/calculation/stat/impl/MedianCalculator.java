package eu.openanalytics.phaedra.calculation.stat.impl;

import org.apache.commons.math.stat.StatUtils;

import eu.openanalytics.phaedra.calculation.stat.ctx.IStatContext;

public class MedianCalculator extends BaseStatCalculator {

	@Override
	public double calculate(IStatContext context) {
		double[] values = context.getData(0);
		return StatUtils.percentile(values, 50);
	}

}
