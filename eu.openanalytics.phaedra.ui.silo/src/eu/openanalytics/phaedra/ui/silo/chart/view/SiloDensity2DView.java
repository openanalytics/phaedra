package eu.openanalytics.phaedra.ui.silo.chart.view;

import static eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractChart.ChartName.AXES_2D;
import static eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractChart.ChartName.DENSITY_2D;
import static eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractChart.ChartName.SELECTION;

import java.util.ArrayList;
import java.util.List;

import eu.openanalytics.phaedra.base.ui.charting.v2.layer.AbstractChartLayer;
import eu.openanalytics.phaedra.silo.vo.Silo;

public class SiloDensity2DView extends SiloView {

	@Override
	public List<AbstractChartLayer<Silo, Silo>> createChartLayers() {
		List<AbstractChartLayer<Silo, Silo>> chartLayers = new ArrayList<AbstractChartLayer<Silo, Silo>>();

		chartLayers.add(createLayer(AXES_2D));
		chartLayers.add(createLayer(DENSITY_2D));
		chartLayers.add(createLayer(SELECTION));

		return chartLayers;
	}

}