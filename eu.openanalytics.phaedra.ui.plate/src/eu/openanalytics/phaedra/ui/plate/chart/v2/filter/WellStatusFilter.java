package eu.openanalytics.phaedra.ui.plate.chart.v2.filter;

import java.util.ArrayList;
import java.util.List;

import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.ui.plate.chart.v2.data.WellDataProvider;

public class WellStatusFilter extends AbstractWellFilter {

	private final static String ACCEPT = "Accepted";
	private final static String REJECT = "Rejected";

	private final static List<String> STATUS_NAMES = new ArrayList<String>() {
		private static final long serialVersionUID = 1613036054731134874L;
		{
			add(ACCEPT);
			add(REJECT);
		}
	};

	public WellStatusFilter(WellDataProvider dataProvider) {
		super("Well Status", dataProvider);
		setFilterItems(STATUS_NAMES);
		List<String> activeItems = new ArrayList<String>();
		activeItems.addAll(STATUS_NAMES);
		setActiveFilterItems(activeItems);
	}

	@Override
	public boolean isActive() {
		return !getActiveFilterItems().containsAll(STATUS_NAMES);
	}

	@Override
	protected String getKey(Well well) {
		return well.getStatus() < 0 ? REJECT : ACCEPT;
	}

}