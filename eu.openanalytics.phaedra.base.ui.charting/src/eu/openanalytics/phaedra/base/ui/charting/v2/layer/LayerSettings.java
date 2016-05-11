package eu.openanalytics.phaedra.base.ui.charting.v2.layer;

import java.io.Serializable;

import eu.openanalytics.phaedra.base.ui.charting.v2.chart.ChartSettings;
import eu.openanalytics.phaedra.base.ui.charting.v2.data.DataProviderSettings;

public class LayerSettings<ENTITY, ITEM> implements Serializable {

	private static final long serialVersionUID = 5117935622440601157L;

	private String chartType;
	private boolean enabled;

	private ChartSettings chartSettings;
	private DataProviderSettings<ENTITY, ITEM> dataProviderSettings;

	public LayerSettings() {
		this.enabled = true;
		this.chartSettings = new ChartSettings();
	}

	public LayerSettings(AbstractChartLayer<ENTITY, ITEM> chartLayer) {
		this.enabled = chartLayer.isEnabled();
		this.chartType = chartLayer.getChartName();
		this.chartSettings = chartLayer.getChartSettings();
		if (chartLayer.getDataProvider() != null) {
			this.dataProviderSettings = chartLayer.getDataProvider().getDataProviderSettings();
		}
	}

	public String getChartType() {
		return chartType;
	}

	public ChartSettings getChartSettings() {
		return chartSettings;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public void setChartSettings(ChartSettings chartSettings) {
		this.chartSettings = chartSettings;
	}

	public DataProviderSettings<ENTITY, ITEM> getDataProviderSettings() {
		return dataProviderSettings;
	}

	public void setDataProviderSettings(DataProviderSettings<ENTITY, ITEM> dataProviderSettings) {
		this.dataProviderSettings = dataProviderSettings;
	}

}