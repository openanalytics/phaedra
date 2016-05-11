package eu.openanalytics.phaedra.base.ui.nattable.summaryrow;

import java.util.Collections;
import java.util.DoubleSummaryStatistics;
import java.util.List;

import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.command.VisualRefreshCommand;
import org.eclipse.nebula.widgets.nattable.summaryrow.ISummaryProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

import eu.openanalytics.phaedra.base.ui.nattable.misc.INatTableMenuContributor;

public class AbstractStatsSummaryProvider implements INatTableMenuContributor {

	public enum SummaryMode {
		MIN, MAX, MEAN, MEDIAN, COUNT, SUM
	}

	private static SummaryMode currentMode = SummaryMode.MEAN;

	public static SummaryMode getSummaryMode() {
		return currentMode;
	}

	public static void setSummaryMode(SummaryMode mode) {
		currentMode = mode;
	}

	@Override
	public void fillMenu(NatTable table, Menu menu) {
		MenuItem menuItem = new MenuItem(menu, SWT.CASCADE);
		menuItem.setText("Summary Stat");
		Menu statMenu = new Menu(menuItem);
		menuItem.setMenu(statMenu);

		for (SummaryMode mode : SummaryMode.values()) {
			menuItem = new MenuItem(statMenu, SWT.RADIO);
			menuItem.setText(mode.toString());
			menuItem.setSelection(mode == AbstractStatsSummaryProvider.getSummaryMode());
			menuItem.addListener(SWT.Selection, e -> {
				AbstractStatsSummaryProvider.setSummaryMode(mode);
				table.doCommand(new VisualRefreshCommand());
			});
		}
	}

	public Object summarize(List<Double> values) {
		switch (currentMode) {
    	case MEDIAN:
    		Collections.sort(values);
    		int middle = values.size() / 2;
    		if (values.size()%2 != 1) middle++;
    		return values.get(middle);
		case MEAN:
			return getSummaryStatistics(values).getAverage();
		case MIN:
			return getSummaryStatistics(values).getMin();
		case MAX:
			return getSummaryStatistics(values).getMax();
		case COUNT:
			return getSummaryStatistics(values).getCount();
		case SUM:
			return getSummaryStatistics(values).getSum();
		default:
			return ISummaryProvider.DEFAULT_SUMMARY_VALUE;
    	}
	}

	private DoubleSummaryStatistics getSummaryStatistics(List<Double> values) {
		return values.stream().mapToDouble(d -> d).summaryStatistics();
	}

}
