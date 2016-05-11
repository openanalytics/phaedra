package eu.openanalytics.phaedra.ui.silo.chart.tooltips;

import eu.openanalytics.phaedra.base.ui.charting.v2.layer.AbstractChartLayer;
import eu.openanalytics.phaedra.silo.vo.Silo;

public class SiloImageChartLayer extends AbstractChartLayer<Silo, Silo> {

	public SiloImageChartLayer() {
		super(new SiloImageTooltipsChart(), new SiloImageTooltipsLegend());
	}

}
