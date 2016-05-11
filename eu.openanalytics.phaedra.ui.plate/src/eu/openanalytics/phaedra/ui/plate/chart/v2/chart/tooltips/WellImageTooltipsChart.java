package eu.openanalytics.phaedra.ui.plate.chart.v2.chart.tooltips;

import eu.openanalytics.phaedra.base.ui.charting.v2.chart.tooltips.TooltipsChart;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import uk.ac.starlink.ttools.plot.TooltipsPlot.ITooltipProvider;

public class WellImageTooltipsChart extends TooltipsChart<Plate, Well> {

	public WellImageTooltipsChart() {
		setName(ChartName.WELL_IMAGE);
	}

	@Override
	protected ITooltipProvider createTooltipProvider() {
		return new WellImageTooltipProvider(getDataProvider());
	}

}