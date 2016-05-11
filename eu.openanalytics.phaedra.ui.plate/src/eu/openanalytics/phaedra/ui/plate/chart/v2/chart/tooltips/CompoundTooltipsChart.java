package eu.openanalytics.phaedra.ui.plate.chart.v2.chart.tooltips;

import eu.openanalytics.phaedra.base.ui.charting.v2.chart.tooltips.TooltipsChart;
import uk.ac.starlink.ttools.plot.TooltipsPlot.ITooltipProvider;

public class CompoundTooltipsChart<ENTITY, ITEM> extends TooltipsChart<ENTITY, ITEM> {

	public CompoundTooltipsChart() {
		setName(ChartName.COMPOUND);
	}

	@Override
	protected ITooltipProvider createTooltipProvider() {
		return new CompoundTooltipProvider<ENTITY, ITEM>(getDataProvider());
	}

}