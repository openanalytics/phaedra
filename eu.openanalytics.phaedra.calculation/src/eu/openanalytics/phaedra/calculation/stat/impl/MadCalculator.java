package eu.openanalytics.phaedra.calculation.stat.impl;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;

import eu.openanalytics.phaedra.calculation.stat.StatUtils;
import eu.openanalytics.phaedra.calculation.stat.ctx.IStatContext;

public class MadCalculator extends BaseStatCalculator {

	@Override
	public double calculate(IStatContext context) {
		double[] values = context.getData(0);
		DescriptiveStatistics stats = StatUtils.createStats(values);
		
		double[] set = stats.getSortedValues();
		double median = stats.getPercentile(50);
		for (int i=0; i<set.length; i++) {
			set[i] = Math.abs(set[i] - median);
		}
		DescriptiveStatistics newStats = StatUtils.createStats(set);
		
		return newStats.getPercentile(50);
	}

}