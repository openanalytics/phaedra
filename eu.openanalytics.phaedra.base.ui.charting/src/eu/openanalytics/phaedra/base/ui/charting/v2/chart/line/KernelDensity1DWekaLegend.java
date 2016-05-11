package eu.openanalytics.phaedra.base.ui.charting.v2.chart.line;

import org.eclipse.swt.widgets.Shell;

import eu.openanalytics.phaedra.base.ui.util.misc.ValueObservable;

public class KernelDensity1DWekaLegend<ENTITY, ITEM> extends Line2DLegend<ENTITY, ITEM> {

	@Override
	public boolean isShowAuxilaryAxes() {
		return false;
	}

	@Override
	public boolean isFilterable() {
		return true;
	}

	@Override
	public void showSettingsDialog(Shell shell, ValueObservable observable) {
		new KernelDensity1DWekaChartSettingsDialog<ENTITY, ITEM>(shell, getLayer(),observable).open();
	}
}