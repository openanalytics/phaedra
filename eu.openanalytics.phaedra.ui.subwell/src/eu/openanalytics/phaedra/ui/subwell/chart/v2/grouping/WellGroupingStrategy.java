package eu.openanalytics.phaedra.ui.subwell.chart.v2.grouping;

import eu.openanalytics.phaedra.base.util.misc.NumberUtils;
import eu.openanalytics.phaedra.model.plate.vo.Well;

public class WellGroupingStrategy extends AbstractSubWellGroupingStrategy {

	@Override
	public String getName() {
		return "Group by well";
	}

	@Override
	protected String getKey(Well well) {
		return NumberUtils.getWellCoordinate(well.getRow(), well.getColumn());
	}

}