package eu.openanalytics.phaedra.ui.subwell.chart.v2.chart.tooltips;

import org.eclipse.swt.widgets.Shell;

import eu.openanalytics.phaedra.base.ui.charting.v2.chart.tooltips.TooltipsLegend;
import eu.openanalytics.phaedra.base.ui.util.misc.ValueObservable;
import eu.openanalytics.phaedra.model.plate.vo.Well;

public class SubWellImageTooltipsLegend extends TooltipsLegend<Well, Well> {

	@Override
	public void showSettingsDialog(Shell shell, ValueObservable observable) {
		new SubWellImageTooltipsSettingsDialog(shell, getLayer()).open();
	}

}