package eu.openanalytics.phaedra.ui.subwell.chart.v2.grouping;

import java.util.BitSet;
import java.util.Map.Entry;

import eu.openanalytics.phaedra.base.ui.charting.v2.data.IDataProvider;
import eu.openanalytics.phaedra.base.ui.charting.v2.grouping.DefaultGroupingStrategy;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import uk.ac.starlink.topcat.BitsRowSubset;

public abstract class AbstractSubWellGroupingStrategy extends DefaultGroupingStrategy<Well, Well> {

	public AbstractSubWellGroupingStrategy() {
		super();
	}

	@Override
	public BitsRowSubset[] groupData(IDataProvider<Well, Well> dataProvider) {
		getGroups().clear();

		BitSet currentFilter = dataProvider.getCurrentFilter();

		int rowCount = dataProvider.getTotalRowCount();
		int size = 0;
		for (Well w : dataProvider.getCurrentEntities()) {
			String key = getKey(w);
			getGroups().putIfAbsent(key, new BitSet(rowCount));
			int amount = dataProvider.getDataSizes().get(w);
			for (int i = 0; i < amount; i++) {
				if (currentFilter.get(size)) {
					getGroups().get(key).set(size);
				}
				size++;
			}
		}

		BitsRowSubset[] subsets = new BitsRowSubset[getGroupCount()];
		int subsetsIterator = 0;
		for (Entry<String, BitSet> entry : getGroups().entrySet()) {
			subsets[subsetsIterator++] = new BitsRowSubset(entry.getKey(), getGroups().get(entry.getKey()));
		}

		return subsets;
	}

	protected abstract String getKey(Well well);

}
