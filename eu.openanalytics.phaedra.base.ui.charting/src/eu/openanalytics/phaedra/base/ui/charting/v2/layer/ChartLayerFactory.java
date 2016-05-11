package eu.openanalytics.phaedra.base.ui.charting.v2.layer;

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;

import eu.openanalytics.phaedra.base.ui.charting.Activator;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractLegend;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractChart.ChartName;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.axes.Axes1DChart;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.axes.Axes1DLegend;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.axes.Axes2DChart;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.axes.Axes2DLegend;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.axes.AxesDynamicChart;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.contour.Contour2DChart;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.contour.ContourLegend;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.density.Density2DChart;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.density.Density2DLegend;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.gates.Gates1DChart;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.gates.GatesChart;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.gates.GatesLegend;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.histogram.Histogram1DChart;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.histogram.Histogram1DLegend;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.line.KernelDensity1DChart;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.line.KernelDensity1DLegend;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.line.KernelDensity1DWekaChart;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.line.KernelDensity1DWekaLegend;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.parallelcoord.ParallelCoordinateChart;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.parallelcoord.ParallelCoordinateLegend;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.scatter.Scatter2DChart;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.scatter.Scatter2DLegend;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.scatter.Scatter3DChart;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.scatter.Scatter3DLegend;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.scatterdensity.ScatterDensity2DChart;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.scatterdensity.ScatterDensity2DLegend;
import eu.openanalytics.phaedra.base.ui.charting.v2.data.IDataProvider;

public abstract class ChartLayerFactory<ENTITY, ITEM> {

	public AbstractChartLayer<ENTITY, ITEM> createLayer(LayerSettings<ENTITY, ITEM> settings, List<ITEM> entities) {
		AbstractChartLayer<ENTITY, ITEM> layer = null;
		if (settings.getChartType() != null) {
			ChartName name = ChartName.valueOf(settings.getChartType());
			layer = createLayer(name);
			if (layer != null) {
				layer.setEnabled(settings.isEnabled());
				layer.setChartSettings(settings.getChartSettings());
				if (layer.getDataProvider() != null) {
					layer.getDataProvider().loadData(entities, layer.getChart().getType().getNumberOfDimensions());
					layer.getDataProvider().setDataProviderSettings(settings.getDataProviderSettings());
				}
			}
		}
		return layer;
	}

	@SuppressWarnings("unchecked")
	public AbstractChartLayer<ENTITY, ITEM> createLayer(ChartName name) {

		IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(Activator.PLUGIN_ID + ".chartLayer");
		try {
			for (IConfigurationElement e : config) {
				if (e.getAttribute("name").equals(name.getDecription())) {
					Object o = e.createExecutableExtension("class");
					if (o instanceof AbstractChartLayer) {
						AbstractChartLayer<ENTITY, ITEM> layer = (AbstractChartLayer<ENTITY, ITEM>) o;
						layer.initializeChartLayer(getDataProvider());
						return layer;
					}
				}
			}
		} catch (CoreException e) {
			Activator.getDefault().getLog().log(new Status(Status.ERROR, Activator.PLUGIN_ID, e.getMessage(), e));
			return null;
		}

		if (name != null) {
			switch (name) {
			case HISTOGRAM_1D:
				return new AbstractChartLayer<>(new Histogram1DChart<ENTITY, ITEM>(), new Histogram1DLegend<ENTITY, ITEM>(), getDataProvider());
			case DENSITY_R_1D:
				return new AbstractChartLayer<>(new KernelDensity1DChart<ENTITY, ITEM>(), new KernelDensity1DLegend<ENTITY, ITEM>(), getDataProvider());
			case DENSITY_1D:
				return new AbstractChartLayer<>(new KernelDensity1DWekaChart<ENTITY, ITEM>(), new KernelDensity1DWekaLegend<ENTITY, ITEM>(), getDataProvider());
			case DENSITY_2D:
				return new AbstractChartLayer<>(new Density2DChart<ENTITY, ITEM>(), new Density2DLegend<ENTITY, ITEM>(), getDataProvider());
			case SCATTER_2D:
				return new AbstractChartLayer<>(new Scatter2DChart<ENTITY, ITEM>(), getLegend(name), getDataProvider());
			case SCATTER_3D:
				return new AbstractChartLayer<>(new Scatter3DChart<ENTITY, ITEM>(), getLegend(name), getDataProvider());
			case PARALLEL_COORDINATES:
				return new AbstractChartLayer<>(new ParallelCoordinateChart<ENTITY, ITEM>(), new ParallelCoordinateLegend<ENTITY, ITEM>(), getDataProvider());
			case CONTOUR_2D:
				return new AbstractChartLayer<>(new Contour2DChart<ENTITY, ITEM>(), new ContourLegend<ENTITY, ITEM>(), getDataProvider());
			case SCATTER_DENSITY_2D:
				return new AbstractChartLayer<>(new ScatterDensity2DChart<ENTITY, ITEM>(), new ScatterDensity2DLegend<ENTITY, ITEM>(), getDataProvider());
			case AXES_1D:
				return new AbstractChartLayer<>(new Axes1DChart<ENTITY, ITEM>(), new Axes1DLegend<ENTITY, ITEM>(), getDataProvider());
			case AXES_2D:
				return new AbstractChartLayer<>(new Axes2DChart<ENTITY, ITEM>(), new Axes2DLegend<ENTITY, ITEM>(), getDataProvider());
			case AXES_DYNAMIC:
				return new AbstractChartLayer<>(new AxesDynamicChart<ENTITY, ITEM>(), new Axes2DLegend<ENTITY, ITEM>(), getDataProvider());
			case GATES:
				return new AbstractChartLayer<>(new GatesChart<ENTITY, ITEM>(), new GatesLegend<ENTITY, ITEM>(), getDataProvider());
			case GATES_1D:
				return new AbstractChartLayer<>(new Gates1DChart<ENTITY, ITEM>(), new GatesLegend<ENTITY, ITEM>(), getDataProvider());
			default:
				return null;
			}
		}
		return null;
	}

	public AbstractLegend<ENTITY, ITEM> getLegend(ChartName name) {
		switch (name) {
		case SCATTER_2D:
			return new Scatter2DLegend<ENTITY, ITEM>();
		case SCATTER_3D:
			return new Scatter3DLegend<ENTITY, ITEM>();
		default:
			return null;
		}
	}

	public abstract IDataProvider<ENTITY, ITEM> getDataProvider();

}