package eu.openanalytics.phaedra.base.ui.charting.v2.util;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractChart.ChartType;
import eu.openanalytics.phaedra.base.ui.charting.v2.layer.AbstractChartLayer;

public class ChartUtils {

	public static <E, I> double[][] calculateMaxBounds(List<AbstractChartLayer<E, I>> chartLayers) {
		return calculateMaxBounds(chartLayers, new NullProgressMonitor());
	}

	public static <E, I> double[][] calculateMaxBounds(List<AbstractChartLayer<E, I>> chartLayers, IProgressMonitor monitor) {
		monitor.beginTask("Calculating Bounds", chartLayers.size());
		int dimensions = getDimensionCount(chartLayers);
		double[][] maxBounds = new double[dimensions][2];
		for (int i = 0; i < dimensions; i++) {
			maxBounds[i][0] = Float.MAX_VALUE;
			maxBounds[i][1] = -Float.MAX_VALUE;
		}
		for (AbstractChartLayer<E, I> layer : chartLayers) {
			if (monitor.isCanceled()) return null;
			if (layer.isDataLayer() && !layer.isAxesLayer() && !layer.isTooltipLayer() && !layer.isSelectionLayer() && !layer.isGateLayer()) {
				double[][] bounds = layer.getDataProvider().calculateDatabounds();
				layer.getDataProvider().setDataBounds(bounds);

				for (int i = 0; i < dimensions; i++) {
					maxBounds[i][0] = Math.min(maxBounds[i][0], bounds[i][0]);
					maxBounds[i][1] = Math.max(maxBounds[i][1], bounds[i][1]);
				}
			}
			monitor.worked(1);
		}

		return maxBounds;
	}

	private static <E, I> int getDimensionCount(List<AbstractChartLayer<E, I>> chartLayers) {
		AbstractChartLayer<E, I> layer = getBottomEnabledDataLayer(chartLayers);
		if (layer == null) {
			return 0;
		} else if (layer.getChart().getType() == ChartType.ONE_DIMENSIONAL) {
			// One dimensional chart still has 2 axes.
			return 2;
		} else if (layer.getChart().getType() == ChartType.DYNAMIC) {
			return layer.getDataProvider().getSelectedFeatures().size();
		}
		return layer.getChart().getType().getNumberOfDimensions();
	}

	public static <E, I> AbstractChartLayer<E, I> getBottomEnabledDataLayer(List<AbstractChartLayer<E, I>> chartLayers) {
		if (chartLayers == null) return null;

		for (int i = 0; i < chartLayers.size(); i++) {
			AbstractChartLayer<E, I> layer = chartLayers.get(i);
			if (layer.isEnabled() && layer.isDataLayer()) {
				return layer;
			}
		}
		return getBottomDataLayer(chartLayers);
	}

	private static <E, I> AbstractChartLayer<E, I> getBottomDataLayer(List<AbstractChartLayer<E, I>> chartLayers) {
		for (int i = 0; i < chartLayers.size(); i++) {
			AbstractChartLayer<E, I> layer = chartLayers.get(i);
			if (layer.isDataLayer()) {
				return layer;
			}
		}
		return null;
	}

}
