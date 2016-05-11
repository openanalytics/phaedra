package eu.openanalytics.phaedra.base.ui.charting.v2.layer;

import java.awt.Component;
import java.awt.Shape;
import java.util.BitSet;

import eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractChart;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractLegend;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.ChartSettings;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.axes.Axes1DChart;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.axes.Axes1DLegend;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.axes.Axes2DChart;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.axes.Axes2DLegend;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.axes.AxesDynamicChart;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.gates.Gates1DChart;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.gates.GatesChart;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.parallelcoord.ParallelCoordinateLegend;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.tooltips.Tooltips3DChart;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.tooltips.TooltipsChart;
import eu.openanalytics.phaedra.base.ui.charting.v2.data.IDataProvider;

public class AbstractChartLayer<ENTITY, ITEM> implements Comparable<AbstractChartLayer<ENTITY, ITEM>> {

	private IDataProvider<ENTITY, ITEM> dataProvider;
	private AbstractChart<ENTITY, ITEM> chart;
	private AbstractLegend<ENTITY, ITEM> legend;
	private int order = 1;
	private boolean enabled = true;
	private ChartSettings chartSettings;

	public AbstractChartLayer(AbstractChart<ENTITY, ITEM> chart, AbstractLegend<ENTITY, ITEM> legend) {
		this.chart = chart;
		this.chartSettings = new ChartSettings();
		this.legend = legend;
	}

	public void initializeChartLayer(IDataProvider<ENTITY, ITEM> dataProvider) {
		this.dataProvider = dataProvider;
		if (chart != null) {
			chart.setLayer(this);
			if (dataProvider != null) {
				dataProvider.setDataCalculator(chart.getDataCalculator());
			}
		}
		if (legend != null) {
			legend.setLayer(this);
		}
	}

	public AbstractChartLayer(AbstractChart<ENTITY, ITEM> chart, AbstractLegend<ENTITY, ITEM> legend, IDataProvider<ENTITY, ITEM> dataProvider) {
		this.chart = chart;
		this.dataProvider = dataProvider;
		this.chartSettings = new ChartSettings();

		if (chart != null) {
			chart.setLayer(this);

			// set the data calculator
			if (dataProvider != null) {
				dataProvider.setDataCalculator(chart.getDataCalculator());
			}
		}
		this.legend = legend;
		if (legend != null) {
			legend.setLayer(this);
		}
	}

	public Component buildChartPanel() {
		Component component = null;
		if (chart != null) {
			component = chart.build();

		}
		return component;
	}

	public void dataChanged() {
		chart.dataChanged();

		if (isEnabled()) {
			showChart();
		} else {
			hideChart();
		}
	}

	public void settingsChanged() {
		chart.settingsChanged();

		if (isEnabled()) {
			showChart();
		} else {
			hideChart();
		}

		getDataProvider().getDataChangedObservable().valueChanged();
	}

	public void hideChart() {
		getChart().hide();
	}

	public void showChart() {
		getChart().show();
	}

	/* getter and setters */
	public IDataProvider<ENTITY, ITEM> getDataProvider() {
		return dataProvider;
	}

	public AbstractChart<ENTITY, ITEM> getChart() {
		return chart;
	}

	public void setChart(AbstractChart<ENTITY, ITEM> chart) {
		this.chart = chart;
	}

	public AbstractLegend<ENTITY, ITEM> getLegend() {
		return legend;
	}

	public void setLegend(AbstractLegend<ENTITY, ITEM> legend) {
		this.legend = legend;
	}

	public int getOrder() {
		return order;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	public ChartSettings getChartSettings() {
		return chartSettings;
	}

	public void setChartSettings(ChartSettings chartSettings) {
		this.chartSettings = chartSettings;
	}

	public String getChartName() {
		if (chart != null) {
			return chart.getName().name();
		}
		return null;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public BitSet getActiveSelection() {
		return getChart().getActiveSelection();
	}

	public BitSet getSelection(Shape shape, boolean isSingleSelection) {
		BitSet selection = getChart().getSelection(shape, isSingleSelection);
		if (selection == null) selection = new BitSet();
		return selection;
	}

	public boolean isDataLayer() {
		return getChart() != null && getDataProvider() != null;
	}

	public boolean isAxesLayer() {
		if (getChart() == null)
			return false;
		// Gates layer is a subclass of Axes2DChart, so check the legend also.
		boolean isAxesChart = getChart() instanceof Axes2DChart || getChart() instanceof Axes1DChart || getChart() instanceof AxesDynamicChart;
		boolean isAxesLegend = getLegend() instanceof Axes2DLegend || getLegend() instanceof Axes1DLegend || getLegend() instanceof ParallelCoordinateLegend;
		return isAxesChart && isAxesLegend;
	}

	public boolean isSelectionLayer() {
		return this instanceof SelectionChartLayer;
	}

	public boolean isTooltipLayer() {
		return getChart() != null && (getChart() instanceof TooltipsChart || getChart() instanceof Tooltips3DChart);
	}

	public boolean isGateLayer() {
		return getChart() != null && (getChart() instanceof GatesChart || getChart() instanceof Gates1DChart);
	}

	@Override
	public int compareTo(AbstractChartLayer<ENTITY, ITEM> other) {
		if (other == null) {
			return 1;
		}
		return this.getOrder() - other.getOrder();
	}
}