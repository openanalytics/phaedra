package eu.openanalytics.phaedra.base.ui.charting.v2.view;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractChart.ChartType;
import eu.openanalytics.phaedra.base.ui.charting.v2.layer.AbstractChartLayer;
import eu.openanalytics.phaedra.base.ui.charting.v2.util.ChartUtils;

public class BaseChartView<ENTITY, ITEM> extends LayeredView<ENTITY, ITEM> {

	public static final String INDEPENDENT_LAYER = "INDEPENDENT_LAYER";

	public BaseChartView(List<AbstractChartLayer<ENTITY, ITEM>> chartLayers) {
		super(chartLayers);
	}

	public void recalculateDataBounds() {
		recalculateDataBounds(new NullProgressMonitor());
	}

	public void recalculateDataBounds(IProgressMonitor monitor) {
		int nrOfLayers = getChartLayers().size();
		monitor.beginTask("Recalculate Bounds", nrOfLayers * 2);
		try {
			// Get max values from layers.
			double[][] maxBounds = ChartUtils.calculateMaxBounds(getChartLayers(), new SubProgressMonitor(monitor, nrOfLayers));

			if (maxBounds == null || monitor.isCanceled()) return;

			// Set max values to all layers.
			setDataBoundsForAllLayers(maxBounds, new SubProgressMonitor(monitor, nrOfLayers));
		} finally {
			monitor.done();
		}
	}

	public int getDimensionCount() {
		if (getChartLayers() != null) {
			AbstractChartLayer<ENTITY, ITEM> layer = getBottomEnabledLayer();
			if (layer != null) {
				if (layer.getChart().getType() == ChartType.ONE_DIMENSIONAL) {
					return 2;
				}
				if (layer.getChart().getType() == ChartType.DYNAMIC) {
					return layer.getDataProvider().getSelectedFeatures().size();
				}
				return layer.getChart().getType().getNumberOfDimensions();
			}
		}
		return 0;
	}

	public void reloadDataForAllLayers(List<ITEM> entities, IProgressMonitor monitor) {
		IProgressMonitor subMonitor = new SubProgressMonitor(monitor, 95);
		List<AbstractChartLayer<ENTITY, ITEM>> chartLayers = getChartLayers();
		for (AbstractChartLayer<ENTITY, ITEM> layer : chartLayers) {
			if (layer.getDataProvider() != null) {
				layer.getDataProvider().loadData(entities, layer.getChart().getType().getNumberOfDimensions(), subMonitor);
				if (subMonitor.isCanceled()) return;
				// Only the first layer uses the real monitor (subsequent layers will use cached data and thus are much faster).
				subMonitor = new NullProgressMonitor();
			}
		}

		if (monitor.isCanceled()) return;
		if (entities != null && !entities.isEmpty()) recalculateDataBounds(new SubProgressMonitor(monitor, 5));
		monitor.done();
	}

	public void dataChangedForAllLayers() {
		for (AbstractChartLayer<ENTITY, ITEM> layer : getChartLayers()) {
			dataChangedForLayer(layer);
		}
	}

	public void dataChangedForLayer(AbstractChartLayer<ENTITY, ITEM> layer) {
		layer.dataChanged();
	}

	public void settingsChangedForLayer(AbstractChartLayer<ENTITY, ITEM> layer) {
		layer.settingsChanged();
	}

	public void setDataBoundsForAllLayers(double[][] bounds) {
		setDataBoundsForAllLayers(bounds, new NullProgressMonitor());
	}

	public void setDataBoundsForAllLayers(double[][] bounds, IProgressMonitor monitor) {
		monitor.beginTask("Setting Bounds", getChartLayers().size());
		try {
			for (AbstractChartLayer<ENTITY, ITEM> layer : getChartLayers()) {
				if (monitor.isCanceled()) return;
				if (layer.isDataLayer()) {
					if (layer.getChartSettings().getMiscSettings().get(INDEPENDENT_LAYER) == null) {
						double[][] originalBounds = layer.getDataProvider().getDataBounds();

						// Copy bounds while keeping auxiliary axes bounds
						for (int i = 0; i < bounds.length && i < originalBounds.length; i++) {
							originalBounds[i] = bounds[i];
						}

						layer.getDataProvider().setDataBounds(originalBounds);
					} else {
						updateAxesLayer();
					}
					dataChangedForLayer(layer);
				}
				monitor.worked(1);
			}
		} finally {
			monitor.done();
		}
	}

	public void setRedraw(boolean isRedraw) {
		// Do nothing.
	}

	protected void updateAxesLayer() {
		// Do nothing.
	}

}