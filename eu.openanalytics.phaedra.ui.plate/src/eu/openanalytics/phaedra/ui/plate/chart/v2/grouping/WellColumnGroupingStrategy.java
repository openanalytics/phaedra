package eu.openanalytics.phaedra.ui.plate.chart.v2.grouping;

import eu.openanalytics.phaedra.model.plate.vo.Well;

public class WellColumnGroupingStrategy extends AbstractWellGroupingStrategy {

	@Override
	public String getName() {
		return "Group by well column";
	}

	@Override
	protected String getKey(Well well) {
		return "C" + well.getColumn();
	}

}