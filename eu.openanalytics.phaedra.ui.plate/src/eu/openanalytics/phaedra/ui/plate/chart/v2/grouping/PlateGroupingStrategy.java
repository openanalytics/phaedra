package eu.openanalytics.phaedra.ui.plate.chart.v2.grouping;

import eu.openanalytics.phaedra.model.plate.vo.Well;

public class PlateGroupingStrategy extends AbstractWellGroupingStrategy {

	@Override
	public String getName() {
		return "Group by plate";
	}

	@Override
	protected String getKey(Well well) {
		return well.getPlate().getBarcode() + "";
	}

}
