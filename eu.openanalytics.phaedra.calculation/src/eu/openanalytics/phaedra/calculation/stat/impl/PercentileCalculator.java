package eu.openanalytics.phaedra.calculation.stat.impl;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;

import eu.openanalytics.phaedra.calculation.stat.StatUtils;
import eu.openanalytics.phaedra.calculation.stat.ctx.ArgumentStatContext;
import eu.openanalytics.phaedra.calculation.stat.ctx.IStatContext;

/**
 * Note: this calculator only works with ArgumentStatContext,
 * where the first argument is the percentage, given as a Double.
 * 
 * In all other cases, this calculator returns NaN.
 */
public class PercentileCalculator extends BaseStatCalculator {

	@Override
	public double calculate(IStatContext context) {
		double percentile = 0;
		if (context instanceof ArgumentStatContext) {
			percentile = (double)((ArgumentStatContext)context).getArg(0);
		} else {
			return Double.NaN;
		}
		if (percentile <= 0 || percentile > 100) return Double.NaN;
		double[] values = context.getData(0);
		DescriptiveStatistics stats = StatUtils.createStats(values);
		return stats.getPercentile(percentile);
	}

}
