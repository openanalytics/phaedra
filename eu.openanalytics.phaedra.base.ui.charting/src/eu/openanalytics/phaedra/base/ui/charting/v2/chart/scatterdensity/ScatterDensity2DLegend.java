package eu.openanalytics.phaedra.base.ui.charting.v2.chart.scatterdensity;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.widgets.Shell;

import eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractLegend;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractLegendItem;
import eu.openanalytics.phaedra.base.ui.util.misc.ValueObservable;

public class ScatterDensity2DLegend<ENTITY, ITEM> extends AbstractLegend<ENTITY, ITEM> {

	@Override
	public List<? extends AbstractLegendItem<ENTITY, ITEM>> createLegendItems() {
		return new ArrayList<>();
	}

	@Override
	public void showSettingsDialog(Shell shell, ValueObservable observable) {
		new ScatterDensityChartSettingsDialog<ENTITY, ITEM>(shell, getLayer()).open();
	}

	@Override
	public boolean isFilterable() {
		return true;
	}

	@Override
	public boolean hasAxesSupport() {
		return true;
	}
}