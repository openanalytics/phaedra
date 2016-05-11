package eu.openanalytics.phaedra.ui.subwell.chart.v2.chart.tooltips;

import eu.openanalytics.phaedra.base.ui.charting.v2.chart.tooltips.TooltipsChart;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import uk.ac.starlink.ttools.plot.TooltipsPlot.ITooltipProvider;

public class SubWellImageTooltipsChart extends TooltipsChart<Well, Well> {

	@Override
	protected ITooltipProvider createTooltipProvider() {
		return new SubWellImageTooltipProvider(getDataProvider());
	}

}