package eu.openanalytics.phaedra.ui.subwell.chart.v2.chart.tooltips;

import eu.openanalytics.phaedra.base.ui.charting.v2.chart.tooltips.Tooltips3DChart;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import uk.ac.starlink.ttools.plot.TooltipsPlot.ITooltipProvider;

public class SubWellImageTooltips3DChart extends Tooltips3DChart<Well, Well> {

	@Override
	protected ITooltipProvider getTooltipProvider() {
		return new SubWellImageTooltipProvider(getDataProvider());
	}

}