package eu.openanalytics.phaedra.base.ui.charting.v2.chart.line;


public class KernelDensity1DLegend<ENTITY, ITEM> extends Line2DLegend<ENTITY, ITEM> {

	@Override
	public boolean isShowAuxilaryAxes() {
		return false;
	}

	@Override
	public boolean isFilterable() {
		return false;
	}
}