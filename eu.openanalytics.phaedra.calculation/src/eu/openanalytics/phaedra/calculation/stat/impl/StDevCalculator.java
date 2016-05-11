package eu.openanalytics.phaedra.calculation.stat.impl;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;

import eu.openanalytics.phaedra.calculation.stat.StatUtils;
import eu.openanalytics.phaedra.calculation.stat.ctx.IStatContext;

public class StDevCalculator extends BaseStatCalculator {

	@Override
	public double calculate(IStatContext context) {
		double[] values = context.getData(0);
		DescriptiveStatistics stats = StatUtils.createStats(values);
		return stats.getStandardDeviation();
	}

}
