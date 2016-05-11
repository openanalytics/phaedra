package eu.openanalytics.phaedra.ui.plate.chart.v2.filter;

import java.util.BitSet;
import java.util.stream.IntStream;

import eu.openanalytics.phaedra.base.ui.charting.v2.filter.AbstractFilter;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.ui.plate.chart.v2.data.WellDataProvider;

public abstract class AbstractWellFilter extends AbstractFilter<Plate, Well> {

	public AbstractWellFilter(String group, WellDataProvider dataProvider) {
		super(group, dataProvider);
	}

	@Override
	public void filter() {
		BitSet currentFilter = getDataProvider().getCurrentFilter();

		int rowCount = getDataProvider().getTotalRowCount();
		IntStream.range(0, rowCount).forEach(index -> {
			if (currentFilter.get(index)) {
				Well w = getDataProvider().getCurrentItems().get(index);
				currentFilter.set(index, getActiveFilterItems().contains(getKey(w)));
			}
		});
	}

	protected abstract String getKey(Well well);

}
