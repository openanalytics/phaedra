package eu.openanalytics.phaedra.ui.plate.chart.v2.view;

import java.util.ArrayList;
import java.util.List;

import eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractChart;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractChart.ChartName;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.scatter.Scatter3DChart;
import eu.openanalytics.phaedra.base.ui.charting.v2.layer.AbstractChartLayer;
import eu.openanalytics.phaedra.base.ui.charting.v2.view.InteractiveChartView;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;

public class WellScatter3DView extends WellView {

	private List<ChartName> wellPossibleLayers = new ArrayList<ChartName>() {
		private static final long serialVersionUID = -1873269205964590654L;
		{
			add(ChartName.SCATTER_3D);
			add(ChartName.SCATTER_DENSITY_3D);
			add(ChartName.CONTOUR_3D);
			add(ChartName.COMPOUND_3D);
		}
	};

	@Override
	public List<AbstractChartLayer<Plate, Well>> createChartLayers() {
		List<AbstractChartLayer<Plate, Well>> chartLayers = new ArrayList<AbstractChartLayer<Plate, Well>>();

		chartLayers.add(createLayer(ChartName.SCATTER_3D));
		chartLayers.add(createLayer(ChartName.SELECTION));

		return chartLayers;
	}

	@Override
	public List<ChartName> getPossibleLayers() {
		return wellPossibleLayers;
	}

	@Override
	public AbstractChartLayer<Plate, Well> createLayer(ChartName name) {
		AbstractChartLayer<Plate, Well> layer = super.createLayer(name);

		AbstractChart<Plate, Well> chart = layer.getChart();
		InteractiveChartView<Plate, Well> chartView = getChartView();
		// Check if the newly created layer is a 3D chart layer.
		if (chart instanceof Scatter3DChart && chartView != null) {
			// If so, get the default rotation.
			double[] rotation = ((Scatter3DChart<Plate, Well>) chart).getRotation();
			double zoom = ((Scatter3DChart<Plate, Well>) chart).getZoom();
			// Loop the already existing layers.
			List<AbstractChartLayer<Plate, Well>> chartLayers = chartView.getChartLayers();
			for (AbstractChartLayer<Plate, Well> currentChartLayer : chartLayers) {
				// If the existing layer is a 3D Chart layer, get its rotation.
				AbstractChart<Plate, Well> currentChart = currentChartLayer.getChart();
				if (currentChart instanceof Scatter3DChart) {
					rotation = ((Scatter3DChart<Plate, Well>) currentChart).getRotation();
					zoom = ((Scatter3DChart<Plate, Well>) currentChart).getZoom();
				}
			}
			// Update the rotation of the new chart to the current charts.
			((Scatter3DChart<Plate, Well>) chart).setRotation(rotation);
			((Scatter3DChart<Plate, Well>) chart).setZoom(zoom);
		}

		return layer;
	}


}