package eu.openanalytics.phaedra.ui.subwell.chart.v2.layer;

import eu.openanalytics.phaedra.base.ui.charting.v2.layer.AbstractChartLayer;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.ui.subwell.chart.v2.chart.tooltips.SubWellImageTooltipsChart;
import eu.openanalytics.phaedra.ui.subwell.chart.v2.chart.tooltips.SubWellImageTooltipsLegend;

public class SubWellImageChartLayer extends AbstractChartLayer<Well, Well> {

	public SubWellImageChartLayer() {
		super(new SubWellImageTooltipsChart(), new SubWellImageTooltipsLegend());
	}

}