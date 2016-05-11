package eu.openanalytics.phaedra.ui.silo.chart.tooltips;

import org.eclipse.swt.widgets.Shell;

import eu.openanalytics.phaedra.base.ui.charting.v2.chart.tooltips.TooltipsLegend;
import eu.openanalytics.phaedra.base.ui.util.misc.ValueObservable;
import eu.openanalytics.phaedra.silo.vo.Silo;

public class SiloImageTooltipsLegend extends TooltipsLegend<Silo, Silo> {

	@Override
	public void showSettingsDialog(Shell shell, ValueObservable observable) {
		new SiloImageTooltipsSettingsDialog(shell, getLayer()).open();
	}

}