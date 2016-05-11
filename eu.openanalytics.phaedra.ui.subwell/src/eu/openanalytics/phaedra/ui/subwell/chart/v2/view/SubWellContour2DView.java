package eu.openanalytics.phaedra.ui.subwell.chart.v2.view;

import java.util.ArrayList;
import java.util.List;

import eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractChart.ChartName;
import eu.openanalytics.phaedra.base.ui.charting.v2.layer.AbstractChartLayer;
import eu.openanalytics.phaedra.model.plate.vo.Well;

public class SubWellContour2DView extends SubWellView {

	@Override
	public List<AbstractChartLayer<Well, Well>> createChartLayers() {
		List<AbstractChartLayer<Well, Well>> chartLayers = new ArrayList<AbstractChartLayer<Well, Well>>();

		chartLayers.add(createLayer(ChartName.AXES_2D));
		chartLayers.add(createLayer(ChartName.CONTOUR_2D));
		chartLayers.add(createLayer(ChartName.SELECTION));

		return chartLayers;
	}
}