package eu.openanalytics.phaedra.ui.subwell.chart.v2.view;

import java.util.ArrayList;
import java.util.List;

import eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractChart;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractChart.ChartName;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.scatter.Scatter3DChart;
import eu.openanalytics.phaedra.base.ui.charting.v2.layer.AbstractChartLayer;
import eu.openanalytics.phaedra.base.ui.charting.v2.view.InteractiveChartView;
import eu.openanalytics.phaedra.model.plate.vo.Well;

public class SubWellScatter3DView extends SubWellView {

	private List<ChartName> wellPossibleLayers = new ArrayList<ChartName>() {
		private static final long serialVersionUID = 7189085085824744881L;
		{
			add(ChartName.SCATTER_3D);
			add(ChartName.SCATTER_DENSITY_3D);
			add(ChartName.CONTOUR_3D);
			add(ChartName.CELL_IMAGE_3D);
			add(ChartName.COMPOUND_3D);
		}
	};

	@Override
	public List<AbstractChartLayer<Well, Well>> createChartLayers() {
		List<AbstractChartLayer<Well, Well>> chartLayers = new ArrayList<AbstractChartLayer<Well, Well>>();
		chartLayers.add(createLayer(ChartName.SCATTER_3D));
		chartLayers.add(createLayer(ChartName.SELECTION));

		return chartLayers;
	}

	@Override
	public List<ChartName> getPossibleLayers() {
		return wellPossibleLayers;
	}

	@Override
	public AbstractChartLayer<Well, Well> createLayer(ChartName name) {
		AbstractChartLayer<Well, Well> layer = super.createLayer(name);

		AbstractChart<Well, Well> chart = layer.getChart();
		InteractiveChartView<Well, Well> chartView = getChartView();
		// Check if the newly created layer is a 3D chart layer.
		if (chart instanceof Scatter3DChart && chartView != null) {
			// If so, get the default rotation.
			double[] rotation = ((Scatter3DChart<Well, Well>) chart).getRotation();
			double zoom = ((Scatter3DChart<Well, Well>) chart).getZoom();
			// Loop the already existing layers.
			List<AbstractChartLayer<Well, Well>> chartLayers = chartView.getChartLayers();
			for (AbstractChartLayer<Well, Well> currentChartLayer : chartLayers) {
				// If the existing layer is a 3D Chart layer, get its rotation.
				AbstractChart<Well, Well> currentChart = currentChartLayer.getChart();
				if (currentChart instanceof Scatter3DChart) {
					rotation = ((Scatter3DChart<Well, Well>) currentChart).getRotation();
					zoom = ((Scatter3DChart<Well, Well>) currentChart).getZoom();
				}
			}
			// Update the rotation of the new chart to the current charts.
			((Scatter3DChart<Well, Well>) chart).setRotation(rotation);
			((Scatter3DChart<Well, Well>) chart).setZoom(zoom);
		}

		return layer;
	}

}