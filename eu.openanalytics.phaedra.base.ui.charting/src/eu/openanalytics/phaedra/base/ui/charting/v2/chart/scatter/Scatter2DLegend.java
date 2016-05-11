package eu.openanalytics.phaedra.base.ui.charting.v2.chart.scatter;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.widgets.Shell;

import eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractLegend;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractLegendItem;
import eu.openanalytics.phaedra.base.ui.charting.v2.data.IDataProvider;
import eu.openanalytics.phaedra.base.ui.charting.v2.grouping.IGroupingStrategy;
import eu.openanalytics.phaedra.base.ui.util.misc.ValueObservable;
import uk.ac.starlink.ttools.plot.Style;

public class Scatter2DLegend<ENTITY, ITEM> extends AbstractLegend<ENTITY, ITEM> {

	@Override
	public List<? extends AbstractLegendItem<ENTITY, ITEM>> createLegendItems() {
		List<Scatter2DLegendItem<ENTITY, ITEM>> legendItems = new ArrayList<Scatter2DLegendItem<ENTITY, ITEM>>();

		IDataProvider<ENTITY, ITEM> dataProvider = getDataProvider();
		List<String> auxiliaryFeatures = dataProvider.getAuxiliaryFeatures();
		if (auxiliaryFeatures != null && !auxiliaryFeatures.isEmpty()) {
			// Get the feature translated by the DataProvider.
			for (int dim = dataProvider.getSelectedFeatures().size(); dim < dataProvider.getDimensionCount(); dim++) {
				legendItems.add(createLegendItem(dataProvider.getSelectedFeature(dim), true, true));
			}
		} else {
			IGroupingStrategy<ENTITY, ITEM> groupingStrategy = dataProvider.getActiveGroupingStrategy();
			Style[] styles = groupingStrategy.getStyles(getChartSettings());
			String[] groupNames = groupingStrategy.getGroupNames();
			for (int i = 0; i < groupNames.length; i++) {
				legendItems.add(createLegendItem(groupNames[i] , !styles[i].getHidePoints(), false));
			}
		}

		return legendItems;
	}

	public Scatter2DLegendItem<ENTITY, ITEM> createLegendItem(String name, boolean enabled, boolean auxilaryData) {
		return new Scatter2DLegendItem<ENTITY, ITEM>(this, name, enabled, auxilaryData);
	}

	@Override
	public void showSettingsDialog(Shell shell, ValueObservable observable) {
		new Scatter2DChartSettingsDialog<ENTITY, ITEM>(shell, getLayer()).open();
	}

	@Override
	public boolean isShowAuxilaryAxes() {
		return true;
	}

	@Override
	public boolean canModify(String property) {
		if (GROUPING.equals(property)) {
			if (getLegendItems() != null) {
				for (AbstractLegendItem<ENTITY, ITEM> item : getLegendItems()) {
					if (item.hasAuxilaryData()) {
						return false;
					}
				}
			}
			return true;
		}
		return super.canModify(property);
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