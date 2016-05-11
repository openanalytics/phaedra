package eu.openanalytics.phaedra.base.ui.charting.v2.view;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import eu.openanalytics.phaedra.base.ui.charting.v2.layer.AbstractChartLayer;
import eu.openanalytics.phaedra.base.ui.charting.v2.layer.SelectionChartLayer;
import eu.openanalytics.phaedra.base.ui.charting.v2.util.ChartUtils;

public class LayeredView<ENTITY, ITEM> {

	private List<AbstractChartLayer<ENTITY, ITEM>> chartLayers;

	public LayeredView(List<AbstractChartLayer<ENTITY, ITEM>> chartLayers) {
		for (AbstractChartLayer<ENTITY, ITEM> layer : chartLayers) {
			addChartLayer(layer);
		}
	}

	public void addChartLayer(AbstractChartLayer<ENTITY, ITEM> layer) {
		if (this.chartLayers == null) {
			this.chartLayers = new ArrayList<AbstractChartLayer<ENTITY, ITEM>>();
		}
		layer.setOrder(chartLayers.size() + 1);
		this.chartLayers.add(layer);

		// sort them according to order ascending (1-> ...)
		Collections.sort(this.chartLayers);
	}

	public void removeChartLayer(AbstractChartLayer<ENTITY, ITEM> layer) {
		if (this.chartLayers != null && layer != null) {
			int order = layer.getOrder() - 1;
			this.chartLayers.remove(layer);

			for (int i = order; i < chartLayers.size(); i++) {
				chartLayers.get(i).setOrder(i + 1);
			}

			// sort them according to order ascending (1-> ...)
			Collections.sort(this.chartLayers);
		}
	}

	public AbstractChartLayer<ENTITY, ITEM> getTopEnabledLayer() {
		for (int i = chartLayers.size() - 1; i >= 0; i--) {
			AbstractChartLayer<ENTITY, ITEM> layer = chartLayers.get(i);
			if (layer.isEnabled()) {
				return layer;
			}
		}
		return null;
	}

	public AbstractChartLayer<ENTITY, ITEM> getBottomEnabledLayer() {
		return ChartUtils.getBottomEnabledDataLayer(chartLayers);
	}

	public SelectionChartLayer<ENTITY, ITEM> getSelectionLayer() {
		for (AbstractChartLayer<ENTITY, ITEM> layer : getChartLayers()) {
			if (layer.isSelectionLayer()) {
				return (SelectionChartLayer<ENTITY, ITEM>) layer;
			}
		}
		return null;
	}

	public AbstractChartLayer<ENTITY, ITEM> getFirstEnabledChartLayer() {
		for (AbstractChartLayer<ENTITY, ITEM> layer : getChartLayers()) {
			if (layer.isEnabled() && layer.isDataLayer() && !layer.isAxesLayer() && !layer.isTooltipLayer() && !layer.isSelectionLayer()) {
				return layer;
			}
		}
		return null;
	}

	/* getter and setters */
	public List<AbstractChartLayer<ENTITY, ITEM>> getChartLayers() {
		return chartLayers;
	}

	public void setChartLayers(List<AbstractChartLayer<ENTITY, ITEM>> chartLayers) {
		this.chartLayers = chartLayers;
	}
}