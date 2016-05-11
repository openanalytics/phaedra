package eu.openanalytics.phaedra.ui.subwell.chart.v2.view;

import java.util.ArrayList;
import java.util.List;

import eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractChart.ChartName;
import eu.openanalytics.phaedra.base.ui.charting.v2.layer.AbstractChartLayer;
import eu.openanalytics.phaedra.model.plate.vo.Well;

public class SubWellDensity1DView extends SubWell1DView {

	private List<ChartName> subWellDensityPossibleLayers = new ArrayList<ChartName>() {
		private static final long serialVersionUID = 7189085085824744881L;
		{
			add(ChartName.DENSITY_R_1D);
			add(ChartName.HISTOGRAM_1D);
		}
	};

	@Override
	public List<AbstractChartLayer<Well, Well>> createChartLayers() {
		List<AbstractChartLayer<Well, Well>> chartLayers = new ArrayList<AbstractChartLayer<Well, Well>>();

		chartLayers.add(createLayer(ChartName.AXES_1D));
		chartLayers.add(createLayer(ChartName.DENSITY_R_1D));
		chartLayers.add(createLayer(ChartName.SELECTION));

		return chartLayers;
	}

	@Override
	public List<ChartName> getPossibleLayers() {
		return subWellDensityPossibleLayers;
	}

}
