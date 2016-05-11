package eu.openanalytics.phaedra.base.ui.charting.v2.chart.density;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.widgets.Shell;

import eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractLegend;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractLegendItem;
import eu.openanalytics.phaedra.base.ui.util.misc.ValueObservable;

public class Density2DLegend<ENTITY, ITEM> extends AbstractLegend<ENTITY, ITEM> {

	@Override
	public List<? extends AbstractLegendItem<ENTITY, ITEM>> createLegendItems() {
		List<Density2DLegendItem<ENTITY, ITEM>> legendItems = new ArrayList<Density2DLegendItem<ENTITY, ITEM>>();
		List<String> auxiliaryFeatures = getDataProvider().getAuxiliaryFeatures();
		if (auxiliaryFeatures != null) {
			for (String feature : auxiliaryFeatures) {
				legendItems.add(new Density2DLegendItem<ENTITY, ITEM>(this, feature, true, true));
			}
		}
		return legendItems;
	}

	@Override
	public void showSettingsDialog(Shell shell, ValueObservable observable) {
		new Density2DChartSettingsDialog<ENTITY, ITEM>(shell, getLayer()).open();
	}

	@Override
	public boolean isFilterable() {
		return true;
	}

	@Override
	public boolean hasAxesSupport() {
		return true;
	}

	/*@Override
	public boolean canModify(String property) {
		return super.canModify(property);
	}*/

}