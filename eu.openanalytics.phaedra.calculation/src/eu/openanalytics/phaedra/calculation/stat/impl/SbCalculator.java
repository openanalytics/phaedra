package eu.openanalytics.phaedra.calculation.stat.impl;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;

import eu.openanalytics.phaedra.calculation.stat.StatUtils;
import eu.openanalytics.phaedra.calculation.stat.ctx.IStatContext;

public class SbCalculator extends BaseStatCalculator {

	@Override
	public double calculate(IStatContext context) {
		if (context.getDataSets() < 3) return Double.NaN;
		double[] lows = context.getData(1);
		double[] highs = context.getData(2);
		
		if (lows == null || lows.length == 0 || highs == null || highs.length == 0)
			return Double.NaN;
		
		DescriptiveStatistics lowStats = StatUtils.createStats(lows);
		DescriptiveStatistics highStats = StatUtils.createStats(highs);
		
		double value = highStats.getMean() / lowStats.getMean();
		return StatUtils.round(value, 2);
	}

}
