package eu.openanalytics.phaedra.base.ui.charting.v2.chart.scatter;

import org.eclipse.swt.widgets.Shell;

import eu.openanalytics.phaedra.base.ui.util.misc.ValueObservable;

public class Scatter3DLegend<ENTITY, ITEM> extends Scatter2DLegend<ENTITY, ITEM>{

	@Override
	public Scatter2DLegendItem<ENTITY, ITEM> createLegendItem(String name, boolean enabled, boolean auxilaryData) {
		return super.createLegendItem(name, enabled, auxilaryData);
	}

	@Override
	public void showSettingsDialog(Shell shell, ValueObservable observable) {
		new Scatter3DChartSettingsDialog<ENTITY, ITEM>(shell, getLayer()).open();
	}

	@Override
	public boolean isFilterable() {
		return true;
	}
}
