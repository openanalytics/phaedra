package eu.openanalytics.phaedra.base.ui.charting.v2.chart.contour;

import java.awt.Color;

import eu.openanalytics.phaedra.base.ui.charting.v2.layer.AbstractChartLayer;

public class Contour3DChartLayer<ENTITY, ITEM> extends AbstractChartLayer<ENTITY, ITEM> {

	public Contour3DChartLayer() {
		super(new Contour3DChart<ENTITY, ITEM>(), new ContourLegend<ENTITY, ITEM>());
		// When rotating, the points should be black by default to prevent confusion.
		getChartSettings().setDefaultColor(Color.BLACK);
	}

}
