package eu.openanalytics.phaedra.calculation.stat.ctx;

import eu.openanalytics.phaedra.calculation.stat.filter.IFilter;
import eu.openanalytics.phaedra.calculation.stat.filter.NaNFilter;

public class MultiploStatContext extends SimpleStatContext {

	public MultiploStatContext(double[][] data) {
		super(data);
	}

	@Override
	public void applyFilter(IFilter filter) {
		// Ignore the NaNFilter, because for multiplo stats, the arrays must be ordered by well and have the same length
		if (filter instanceof NaNFilter) return;
	}

}
