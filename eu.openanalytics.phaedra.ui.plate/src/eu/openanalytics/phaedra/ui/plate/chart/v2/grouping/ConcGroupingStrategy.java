package eu.openanalytics.phaedra.ui.plate.chart.v2.grouping;

import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Well;

public class ConcGroupingStrategy extends AbstractWellGroupingStrategy {

	@Override
	public String getName() {
		return "Group by concentration";
	}

	@Override
	protected String getKey(Well well) {
		String key = "" + well.getCompoundConcentration();

		if (!PlateUtils.isSample(well)) key = well.getWellType();

		return key;
	}

}
