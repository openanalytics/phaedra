package eu.openanalytics.phaedra.ui.silo.chart.view;

import static eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractChart.ChartName.COMPOUND_3D;
import static eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractChart.ChartName.CONTOUR_3D;
import static eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractChart.ChartName.SCATTER_3D;
import static eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractChart.ChartName.SCATTER_DENSITY_3D;
import static eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractChart.ChartName.SELECTION;
import static eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractChart.ChartName.SILO_IMAGE_3D;

import java.util.ArrayList;
import java.util.List;

import eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractChart;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractChart.ChartName;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.scatter.Scatter3DChart;
import eu.openanalytics.phaedra.base.ui.charting.v2.layer.AbstractChartLayer;
import eu.openanalytics.phaedra.base.ui.charting.v2.view.InteractiveChartView;
import eu.openanalytics.phaedra.silo.vo.Silo;

public class SiloScatter3DView extends SiloView {

	private List<ChartName> wellPossibleLayers = new ArrayList<ChartName>() {
		private static final long serialVersionUID = 7189085085824744881L;
		{
			add(SCATTER_3D);
			add(SCATTER_DENSITY_3D);
			add(CONTOUR_3D);
			add(SILO_IMAGE_3D);
			add(COMPOUND_3D);
		}
	};

	@Override
	public List<ChartName> getPossibleLayers() {
		return wellPossibleLayers;
	}

	@Override
	public List<AbstractChartLayer<Silo, Silo>> createChartLayers() {
		List<AbstractChartLayer<Silo, Silo>> chartLayers = new ArrayList<AbstractChartLayer<Silo, Silo>>();

		chartLayers.add(createLayer(SCATTER_3D));
		chartLayers.add(createLayer(SELECTION));

		return chartLayers;
	}

	@Override
	public AbstractChartLayer<Silo, Silo> createLayer(ChartName name) {
		AbstractChartLayer<Silo, Silo> layer = super.createLayer(name);

		AbstractChart<Silo, Silo> chart = layer.getChart();
		InteractiveChartView<Silo, Silo> chartView = getChartView();
		// Check if the newly created layer is a 3D chart layer.
		if (chart instanceof Scatter3DChart && chartView != null) {
			// If so, get the default rotation.
			double[] rotation = ((Scatter3DChart<Silo, Silo>) chart).getRotation();
			double zoom = ((Scatter3DChart<Silo, Silo>) chart).getZoom();
			// Loop the already existing layers.
			List<AbstractChartLayer<Silo, Silo>> chartLayers = chartView.getChartLayers();
			for (AbstractChartLayer<Silo, Silo> currentChartLayer : chartLayers) {
				// If the existing layer is a 3D Chart layer, get its rotation.
				AbstractChart<Silo, Silo> currentChart = currentChartLayer.getChart();
				if (currentChart instanceof Scatter3DChart) {
					rotation = ((Scatter3DChart<Silo, Silo>) currentChart).getRotation();
					zoom = ((Scatter3DChart<Silo, Silo>) currentChart).getZoom();
				}
			}
			// Update the rotation of the new chart to the current charts.
			((Scatter3DChart<Silo, Silo>) chart).setRotation(rotation);
			((Scatter3DChart<Silo, Silo>) chart).setZoom(zoom);
		}

		return layer;
	}

}