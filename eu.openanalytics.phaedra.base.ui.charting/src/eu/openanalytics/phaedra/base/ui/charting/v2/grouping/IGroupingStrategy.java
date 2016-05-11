package eu.openanalytics.phaedra.base.ui.charting.v2.grouping;

import java.util.BitSet;
import java.util.Map;

import eu.openanalytics.phaedra.base.ui.charting.v2.chart.ChartSettings;
import eu.openanalytics.phaedra.base.ui.charting.v2.data.IDataProvider;
import uk.ac.starlink.topcat.BitsRowSubset;
import uk.ac.starlink.ttools.plot.Style;

public interface IGroupingStrategy<ENTITY, ITEM> {

	String getName();

	Style[] getStyles(ChartSettings settings);

	BitsRowSubset[] groupData(IDataProvider<ENTITY, ITEM> dataProvider);

	String[] getGroupNames();

	Map<String, BitSet> getGroups();

	int getGroupCount();

}
