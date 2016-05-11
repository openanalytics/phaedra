package eu.openanalytics.phaedra.ui.subwell.chart.v2.filter;

import java.util.BitSet;

import eu.openanalytics.phaedra.base.ui.charting.v2.filter.AbstractFilter;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.ui.subwell.chart.v2.data.SubWellDataProvider;

/**
 * Base class for SubWellItem filters that filter on Well Properties.
 *
 * The filter method loops only over each Well instead of each SubWellitem.
 * Because of this, the class can not be used for SubWellItem filters that filter
 * on SubWellItem properties.
 */
public abstract class AbstractSubWellFilter extends AbstractFilter<Well, Well> {

	public AbstractSubWellFilter(String group, SubWellDataProvider dataProvider) {
		super(group, dataProvider);
	}

	@Override
	public void filter() {
		BitSet currentFilter = getDataProvider().getCurrentFilter();

		int size = 0;
		for (Well w : getDataProvider().getCurrentEntities()) {
			int amount = getDataProvider().getDataSizes().get(w);
			if (amount != 0) {
				if (!getActiveFilterItems().contains(getKey(w))) {
					currentFilter.set(size, size + amount, false);
				}
				size += amount;
			}
		}
	}

	protected abstract String getKey(Well well);

}
