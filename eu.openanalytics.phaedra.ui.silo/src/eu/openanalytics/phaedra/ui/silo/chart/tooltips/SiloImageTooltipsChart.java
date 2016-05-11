package eu.openanalytics.phaedra.ui.silo.chart.tooltips;

import static eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractChart.ChartName.SILO_IMAGE;

import eu.openanalytics.phaedra.base.ui.charting.v2.chart.tooltips.TooltipsChart;
import eu.openanalytics.phaedra.silo.vo.Silo;
import eu.openanalytics.phaedra.ui.silo.chart.data.SiloDataProvider;
import uk.ac.starlink.ttools.plot.TooltipsPlot.ITooltipProvider;

public class SiloImageTooltipsChart extends TooltipsChart<Silo, Silo> {

	public SiloImageTooltipsChart() {
		setName(SILO_IMAGE);
	}

	@Override
	protected ITooltipProvider createTooltipProvider() {
		return new SiloImageTooltipProvider((SiloDataProvider) getDataProvider());
	}

}