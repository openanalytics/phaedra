package eu.openanalytics.phaedra.ui.silo.chart.view;

import static eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractChart.ChartName.AXES_1D;
import static eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractChart.ChartName.DENSITY_1D;
import static eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractChart.ChartName.HISTOGRAM_1D;
import static eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractChart.ChartName.SELECTION;

import java.util.ArrayList;
import java.util.List;

import eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractChart.ChartName;
import eu.openanalytics.phaedra.base.ui.charting.v2.layer.AbstractChartLayer;
import eu.openanalytics.phaedra.silo.vo.Silo;

public class SiloDensityWeka1DView extends SiloView {

	private List<ChartName> wellPossibleLayers = new ArrayList<ChartName>() {
		private static final long serialVersionUID = 7189085085824744881L;
		{
			add(DENSITY_1D);
			add(HISTOGRAM_1D);
		}
	};

	@Override
	public List<AbstractChartLayer<Silo, Silo>> createChartLayers() {
		List<AbstractChartLayer<Silo, Silo>> chartLayers = new ArrayList<AbstractChartLayer<Silo, Silo>>();
		chartLayers.add(createLayer(AXES_1D));
		chartLayers.add(createLayer(DENSITY_1D));
		chartLayers.add(createLayer(SELECTION));
		return chartLayers;
	}

	@Override
	public List<ChartName> getPossibleLayers() {
		return wellPossibleLayers;
	}

}