package eu.openanalytics.phaedra.ui.plate.chart.v2.grouping;

import java.util.BitSet;
import java.util.Map.Entry;
import java.util.stream.IntStream;

import eu.openanalytics.phaedra.base.ui.charting.v2.data.IDataProvider;
import eu.openanalytics.phaedra.base.ui.charting.v2.grouping.DefaultGroupingStrategy;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import uk.ac.starlink.topcat.BitsRowSubset;

public abstract class AbstractWellGroupingStrategy extends DefaultGroupingStrategy<Plate, Well> {

	public AbstractWellGroupingStrategy() {
		super();
	}

	@Override
	public BitsRowSubset[] groupData(IDataProvider<Plate, Well> dataProvider) {
		getGroups().clear();

		BitSet currentFilter = dataProvider.getCurrentFilter();

		int rowCount = dataProvider.getTotalRowCount();
		IntStream.range(0, rowCount).forEach(index -> {
			if (currentFilter.get(index)) {
				Well w = dataProvider.getCurrentItems().get(index);
				String group = getKey(w);
				getGroups().putIfAbsent(group, new BitSet(rowCount));
				getGroups().get(group).set(index);
			}
		});

		BitsRowSubset[] subsets = new BitsRowSubset[getGroupCount()];
		int subsetsIterator = 0;
		for (Entry<String, BitSet> entry : getGroups().entrySet()) {
			subsets[subsetsIterator++] = new BitsRowSubset(entry.getKey(), getGroups().get(entry.getKey()));
		}

		return subsets;
	}

	protected abstract String getKey(Well well);

}
