package eu.openanalytics.phaedra.calculation.stat.impl;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;

import eu.openanalytics.phaedra.calculation.stat.StatUtils;
import eu.openanalytics.phaedra.calculation.stat.ctx.IStatContext;

public class RobustZPrimeCalculator extends BaseStatCalculator {

	@Override
	public double calculate(IStatContext context) {
		if (context.getDataSets() < 3) return Double.NaN;
		double[] lows = context.getData(1);
		double[] highs = context.getData(2);
		
		if (lows == null || lows.length == 0 || highs == null || highs.length == 0) return Double.NaN;
		
		DescriptiveStatistics lowStats = StatUtils.createStats(lows);
		DescriptiveStatistics highStats = StatUtils.createStats(highs);

		double highMad = getMAD(highStats);
		double lowMad = getMAD(lowStats);
		double numerator = 3 * 1.4826 * (highMad + lowMad);
		double denominator = Math.abs(highStats.getPercentile(50) - lowStats.getPercentile(50));
		return StatUtils.round(1 - (numerator / denominator), 2);
	}

	private double getMAD(DescriptiveStatistics stats) {
		double[] sortedValues = stats.getSortedValues();
		double median = stats.getPercentile(50);
		for (int i=0; i<sortedValues.length; i++) {
			sortedValues[i] = Math.abs(sortedValues[i] - median);
		}
		DescriptiveStatistics absoluteDeviations = StatUtils.createStats(sortedValues);
		return absoluteDeviations.getPercentile(50);
	}
}
