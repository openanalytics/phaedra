package eu.openanalytics.phaedra.ui.plate.chart.v2.view;

import java.util.ArrayList;
import java.util.List;

import eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractChart.ChartName;
import eu.openanalytics.phaedra.base.ui.charting.v2.layer.AbstractChartLayer;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;

public class WellScatterDensity2DView extends WellView {

	@Override
	public List<AbstractChartLayer<Plate, Well>> createChartLayers() {
		List<AbstractChartLayer<Plate, Well>> chartLayers = new ArrayList<AbstractChartLayer<Plate, Well>>();

		chartLayers.add(createLayer(ChartName.AXES_2D));
		chartLayers.add(createLayer(ChartName.SCATTER_DENSITY_2D));
		chartLayers.add(createLayer(ChartName.SELECTION));

		return chartLayers;
	}

}
