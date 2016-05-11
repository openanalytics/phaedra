package eu.openanalytics.phaedra.base.ui.charting.v2.grouping;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

import eu.openanalytics.phaedra.base.ui.charting.v2.chart.ChartSettings;
import eu.openanalytics.phaedra.base.ui.charting.v2.data.IDataProvider;
import uk.ac.starlink.topcat.BitsRowSubset;
import uk.ac.starlink.ttools.plot.Style;

public class DefaultGroupingStrategy<ENTITY, ITEM> implements IGroupingStrategy<ENTITY, ITEM> {

	public static final String DEFAULT_GROUPING_NAME = "No Grouping";

	private IStyleProvider styleProvider;
	private Map<String, BitSet> groups;

	public DefaultGroupingStrategy(IStyleProvider styleProvider) {
		this.styleProvider = styleProvider;
		this.groups = new HashMap<String, BitSet>();
		// Default grouping is null
		groups.put("All", null);
	}

	public DefaultGroupingStrategy() {
		this(new DefaultStyleProvider());
	}

	@Override
	public String getName() {
		return DEFAULT_GROUPING_NAME;
	}

	@Override
	public Style[] getStyles(ChartSettings settings) {
		return getStyleProvider().getStyles(getGroupNames(), settings);
	}

	@Override
	public String[] getGroupNames() {
		return groups.keySet().toArray(new String[getGroupCount()]);
	}

	/* getter and setters */
	private synchronized IStyleProvider getStyleProvider() {
		return styleProvider;
	}

	public synchronized void setStyleProvider(IStyleProvider styleProvider) {
		this.styleProvider = styleProvider;
	}

	@Override
	public synchronized int getGroupCount() {
		return groups.size();
	}

	@Override
	public synchronized Map<String, BitSet> getGroups() {
		return groups;
	}

	// TODO Introduce doGroupData method to omit getGroups().clear(); and ...!!!
	@Override
	public BitsRowSubset[] groupData(IDataProvider<ENTITY, ITEM> dataProvider) {
		return null;
	}

}