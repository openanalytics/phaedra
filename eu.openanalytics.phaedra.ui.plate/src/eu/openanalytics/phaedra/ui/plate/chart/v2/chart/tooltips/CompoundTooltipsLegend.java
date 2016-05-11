package eu.openanalytics.phaedra.ui.plate.chart.v2.chart.tooltips;

import org.eclipse.swt.widgets.Shell;

import eu.openanalytics.phaedra.base.ui.charting.v2.chart.tooltips.TooltipsLegend;
import eu.openanalytics.phaedra.base.ui.util.misc.ValueObservable;

public class CompoundTooltipsLegend<ENTITY, ITEM> extends TooltipsLegend<ENTITY, ITEM> {

	@Override
	public void showSettingsDialog(Shell shell, ValueObservable observable) {
		new CompoundTooltipsSettingsDialog<ENTITY, ITEM>(shell, getLayer()).open();
	}

}