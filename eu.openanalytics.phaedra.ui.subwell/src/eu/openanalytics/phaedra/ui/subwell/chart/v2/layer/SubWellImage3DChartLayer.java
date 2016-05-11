package eu.openanalytics.phaedra.ui.subwell.chart.v2.layer;

import eu.openanalytics.phaedra.base.ui.charting.v2.layer.AbstractChartLayer;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.ui.subwell.chart.v2.chart.tooltips.SubWellImageTooltips3DChart;
import eu.openanalytics.phaedra.ui.subwell.chart.v2.chart.tooltips.SubWellImageTooltipsLegend;

public class SubWellImage3DChartLayer extends AbstractChartLayer<Well, Well> {

	public SubWellImage3DChartLayer() {
		super(new SubWellImageTooltips3DChart(), new SubWellImageTooltipsLegend());
	}

}