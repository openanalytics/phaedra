package eu.openanalytics.phaedra.ui.plate.chart.v2.layer;

import eu.openanalytics.phaedra.base.ui.charting.v2.layer.AbstractChartLayer;
import eu.openanalytics.phaedra.ui.plate.chart.v2.chart.tooltips.CompoundTooltipsChart;
import eu.openanalytics.phaedra.ui.plate.chart.v2.chart.tooltips.CompoundTooltipsLegend;

public class CompoundChartLayer<ENTITY, ITEM> extends AbstractChartLayer<ENTITY, ITEM> {

	public CompoundChartLayer() {
		super(new CompoundTooltipsChart<ENTITY, ITEM>(), new CompoundTooltipsLegend<ENTITY, ITEM>());
	}

}