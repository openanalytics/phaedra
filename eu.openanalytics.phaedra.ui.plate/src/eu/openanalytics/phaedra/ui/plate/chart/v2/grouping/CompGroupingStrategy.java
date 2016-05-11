package eu.openanalytics.phaedra.ui.plate.chart.v2.grouping;

import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Compound;
import eu.openanalytics.phaedra.model.plate.vo.Well;

public class CompGroupingStrategy extends AbstractWellGroupingStrategy {

	@Override
	public String getName() {
		return "Group by compound";
	}

	@Override
	protected String getKey(Well well) {
		String key = "None";
		Compound comp = well.getCompound();
		if (comp != null) key = comp.getType() + " " + comp.getNumber();
		if (!PlateUtils.isSample(well)) key = well.getWellType();
		return key;
	}

}