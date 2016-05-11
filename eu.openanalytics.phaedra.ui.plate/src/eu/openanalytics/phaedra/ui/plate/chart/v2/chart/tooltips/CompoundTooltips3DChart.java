package eu.openanalytics.phaedra.ui.plate.chart.v2.chart.tooltips;

import eu.openanalytics.phaedra.base.ui.charting.v2.chart.tooltips.Tooltips3DChart;
import uk.ac.starlink.ttools.plot.TooltipsPlot.ITooltipProvider;

public class CompoundTooltips3DChart<ENTITY, ITEM> extends Tooltips3DChart<ENTITY, ITEM> {

	public CompoundTooltips3DChart() {
		setName(ChartName.COMPOUND_3D);
	}

	@Override
	protected ITooltipProvider getTooltipProvider() {
		return new CompoundTooltipProvider<ENTITY, ITEM>(getDataProvider());
	}

}