package eu.openanalytics.phaedra.ui.plate.chart.v2.chart.tooltips;

import org.eclipse.swt.widgets.Shell;

import eu.openanalytics.phaedra.base.ui.charting.v2.chart.tooltips.TooltipsLegend;
import eu.openanalytics.phaedra.base.ui.util.misc.ValueObservable;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;

public class WellImageTooltipsLegend extends TooltipsLegend<Plate, Well> {

	@Override
	public void showSettingsDialog(Shell shell, ValueObservable observable) {
		new WellImageTooltipsSettingsDialog(shell, getLayer()).open();
	}

}