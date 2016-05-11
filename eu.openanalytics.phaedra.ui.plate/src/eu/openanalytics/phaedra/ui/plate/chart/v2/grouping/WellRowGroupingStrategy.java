package eu.openanalytics.phaedra.ui.plate.chart.v2.grouping;

import eu.openanalytics.phaedra.model.plate.vo.Well;

public class WellRowGroupingStrategy extends AbstractWellGroupingStrategy {

	@Override
	public String getName() {
		return "Group by well row";
	}

	@Override
	protected String getKey(Well well) {
		return "R" + well.getRow();
	}

}