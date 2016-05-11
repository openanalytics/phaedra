package eu.openanalytics.phaedra.ui.plate.chart.v2.layer;

import eu.openanalytics.phaedra.base.ui.charting.v2.layer.AbstractChartLayer;
import eu.openanalytics.phaedra.ui.plate.chart.v2.chart.tooltips.CompoundTooltips3DChart;
import eu.openanalytics.phaedra.ui.plate.chart.v2.chart.tooltips.CompoundTooltipsLegend;

public class CompoundChart3DLayer<ENTITY, ITEM> extends AbstractChartLayer<ENTITY, ITEM> {

	public CompoundChart3DLayer() {
		super(new CompoundTooltips3DChart<ENTITY, ITEM>(), new CompoundTooltipsLegend<ENTITY, ITEM>());
	}

}