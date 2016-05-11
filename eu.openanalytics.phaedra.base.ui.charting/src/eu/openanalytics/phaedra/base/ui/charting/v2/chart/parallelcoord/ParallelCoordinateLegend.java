package eu.openanalytics.phaedra.base.ui.charting.v2.chart.parallelcoord;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.widgets.Shell;

import eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractLegendItem;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.scatter.Scatter2DLegend;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.scatter.Scatter2DLegendItem;
import eu.openanalytics.phaedra.base.ui.charting.v2.grouping.IGroupingStrategy;
import eu.openanalytics.phaedra.base.ui.util.misc.ValueObservable;
import uk.ac.starlink.ttools.plot.Style;

public class ParallelCoordinateLegend<ENTITY, ITEM> extends Scatter2DLegend<ENTITY, ITEM> {

	@Override
	public List<? extends AbstractLegendItem<ENTITY, ITEM>> createLegendItems() {
		List<Scatter2DLegendItem<ENTITY, ITEM>> legendItems = new ArrayList<Scatter2DLegendItem<ENTITY, ITEM>>();
		IGroupingStrategy<ENTITY, ITEM> groupingStrategy = getDataProvider().getActiveGroupingStrategy();
		Style[] styles = groupingStrategy.getStyles(getChartSettings());
		String[] groupNames = groupingStrategy.getGroupNames();
		for (int i = 0; i < groupNames.length; i++) {
			legendItems.add(createLegendItem(groupNames[i] , !styles[i].getHidePoints(), false));
		}
		return legendItems;
	}

	@Override
	public void showSettingsDialog(Shell shell, ValueObservable observable) {
		new ParallelCoordinateChartSettingsDialog<ENTITY, ITEM>(shell, getLayer()).open();
	}

	@Override
	public boolean isShowAuxilaryAxes() {
		return false;
	}

}