package eu.openanalytics.phaedra.ui.silo.chart.view;

import static eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractChart.ChartName.AXES_DYNAMIC;
import static eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractChart.ChartName.PARALLEL_COORDINATES;
import static eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractChart.ChartName.SELECTION;

import java.util.ArrayList;
import java.util.List;

import eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractChart.ChartName;
import eu.openanalytics.phaedra.base.ui.charting.v2.layer.AbstractChartLayer;
import eu.openanalytics.phaedra.silo.vo.Silo;

public class SiloParallelCoordinatesView extends SiloView {

	private List<ChartName> wellPossibleLayers = new ArrayList<ChartName>() {
		private static final long serialVersionUID = 7189085085824744881L;
		{
			add(PARALLEL_COORDINATES);
		}
	};

	@Override
	public List<AbstractChartLayer<Silo, Silo>> createChartLayers() {
		List<AbstractChartLayer<Silo, Silo>> chartLayers = new ArrayList<AbstractChartLayer<Silo, Silo>>();
		chartLayers.add(createLayer(AXES_DYNAMIC));
		chartLayers.add(createLayer(PARALLEL_COORDINATES));
		chartLayers.add(createLayer(SELECTION));
		return chartLayers;
	}

	@Override
	public List<ChartName> getPossibleLayers() {
		return wellPossibleLayers;
	}

}