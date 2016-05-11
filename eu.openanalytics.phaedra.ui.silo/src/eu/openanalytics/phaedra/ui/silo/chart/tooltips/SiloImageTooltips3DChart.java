package eu.openanalytics.phaedra.ui.silo.chart.tooltips;

import static eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractChart.ChartName.SILO_IMAGE_3D;

import eu.openanalytics.phaedra.base.ui.charting.v2.chart.tooltips.Tooltips3DChart;
import eu.openanalytics.phaedra.silo.vo.Silo;
import eu.openanalytics.phaedra.ui.silo.chart.data.SiloDataProvider;
import uk.ac.starlink.ttools.plot.TooltipsPlot.ITooltipProvider;

public class SiloImageTooltips3DChart extends Tooltips3DChart<Silo, Silo> {

	public SiloImageTooltips3DChart() {
		setName(SILO_IMAGE_3D);
	}

	@Override
	protected ITooltipProvider getTooltipProvider() {
		return new SiloImageTooltipProvider((SiloDataProvider) getDataProvider());
	}

}