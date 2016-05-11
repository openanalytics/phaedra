package eu.openanalytics.phaedra.base.ui.charting.v2.chart.axes;

import java.util.Collections;
import java.util.List;

import org.eclipse.swt.widgets.Shell;

import eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractLegend;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractLegendItem;
import eu.openanalytics.phaedra.base.ui.util.misc.ValueObservable;

public class Axes1DLegend<ENTITY, ITEM> extends AbstractLegend<ENTITY, ITEM> {

	@Override
	public List<? extends AbstractLegendItem<ENTITY, ITEM>> createLegendItems() {
		return Collections.emptyList();
	}

	//TODO override with 1D version??
	@Override
	public void showSettingsDialog(Shell shell, ValueObservable observable) {
		new Axes2DChartSettingsDialog<ENTITY, ITEM>(shell, getLayer()).open();
	}
}
