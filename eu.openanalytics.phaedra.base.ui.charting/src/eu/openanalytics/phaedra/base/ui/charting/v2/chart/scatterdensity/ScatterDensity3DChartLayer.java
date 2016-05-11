package eu.openanalytics.phaedra.base.ui.charting.v2.chart.scatterdensity;

import eu.openanalytics.phaedra.base.ui.charting.v2.layer.AbstractChartLayer;

public class ScatterDensity3DChartLayer<ENTITY, ITEM> extends AbstractChartLayer<ENTITY, ITEM> {

	public ScatterDensity3DChartLayer() {
		super(new ScatterDensity3DChart<ENTITY, ITEM>(), new ScatterDensity2DLegend<ENTITY, ITEM>());
	}

}
