package eu.openanalytics.phaedra.ui.plate.chart.v2.layer;

import eu.openanalytics.phaedra.base.ui.charting.v2.layer.AbstractChartLayer;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.ui.plate.chart.v2.chart.tooltips.WellImageTooltipsChart;
import eu.openanalytics.phaedra.ui.plate.chart.v2.chart.tooltips.WellImageTooltipsLegend;

public class WellImageChartLayer extends AbstractChartLayer<Plate, Well> {

	public WellImageChartLayer() {
		super(new WellImageTooltipsChart(), new WellImageTooltipsLegend());
	}

}