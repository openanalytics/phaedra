package eu.openanalytics.phaedra.base.ui.charting.v2.chart.histogram;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.widgets.Shell;

import eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractLegend;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractLegendItem;
import eu.openanalytics.phaedra.base.ui.charting.v2.grouping.IGroupingStrategy;
import eu.openanalytics.phaedra.base.ui.util.misc.ValueObservable;
import uk.ac.starlink.ttools.plot.Style;

public class Histogram1DLegend<ENTITY, ITEM> extends AbstractLegend<ENTITY, ITEM> {

	@Override
	public List<? extends AbstractLegendItem<ENTITY, ITEM>> createLegendItems() {
		List<Histogram1DLegendItem<ENTITY, ITEM>> legendItems = new ArrayList<Histogram1DLegendItem<ENTITY, ITEM>>();
		IGroupingStrategy<ENTITY, ITEM> groupingStrategy = getDataProvider().getActiveGroupingStrategy();
		Style[] styles = groupingStrategy.getStyles(getChartSettings());
		String[] groupNames = groupingStrategy.getGroupNames();
		for (int i = 0; i < groupNames.length; i++) {
			legendItems.add(createLegendItem(groupNames[i], !styles[i].getHidePoints(), false));
		}
		return legendItems;
	}

	public Histogram1DLegendItem<ENTITY, ITEM> createLegendItem(String name, boolean enabled, boolean auxilaryData) {
		return new Histogram1DLegendItem<ENTITY, ITEM>(this, name, enabled, auxilaryData);
	}

	@Override
	public void showSettingsDialog(Shell shell, ValueObservable observable) {
		new Histogram1DChartSettingsDialog<ENTITY, ITEM>(shell, getLayer(), observable).open();
	}

	@Override
	public boolean canModify(String property) {
		return GROUPING.equals(property) || super.canModify(property);
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
