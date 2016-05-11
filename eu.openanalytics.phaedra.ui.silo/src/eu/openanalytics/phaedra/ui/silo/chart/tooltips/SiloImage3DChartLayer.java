package eu.openanalytics.phaedra.ui.silo.chart.tooltips;

import eu.openanalytics.phaedra.base.ui.charting.v2.layer.AbstractChartLayer;
import eu.openanalytics.phaedra.silo.vo.Silo;

public class SiloImage3DChartLayer extends AbstractChartLayer<Silo, Silo> {

	public SiloImage3DChartLayer() {
		super(new SiloImageTooltips3DChart(), new SiloImageTooltipsLegend());
	}

}
