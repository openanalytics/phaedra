package eu.openanalytics.phaedra.ui.silo.chart.grouping;

import java.util.BitSet;
import java.util.Map.Entry;

import eu.openanalytics.phaedra.base.ui.charting.v2.data.IDataProvider;
import eu.openanalytics.phaedra.base.ui.charting.v2.grouping.DefaultGroupingStrategy;
import eu.openanalytics.phaedra.silo.vo.Silo;
import eu.openanalytics.phaedra.ui.silo.chart.data.SiloDataProvider;
import uk.ac.starlink.topcat.BitsRowSubset;

public class SiloWellGroupingStrategy extends DefaultGroupingStrategy<Silo, Silo> {

	@Override
	public String getName() {
		return "Group by Well ID";
	}

	@Override
	public BitsRowSubset[] groupData(IDataProvider<Silo, Silo> dataProvider) {
		getGroups().clear();

		SiloDataProvider siloDataProvider = (SiloDataProvider) dataProvider;

		int rowCount = dataProvider.getTotalRowCount();
		for (int i = 0; i < rowCount; i++) {
			long wellId = siloDataProvider.getWell(i).getId();
			String key = String.valueOf(wellId);
			
			if (!getGroups().containsKey(key)) {
				BitSet bitSet = new BitSet(rowCount);
				getGroups().put(key, bitSet);
			}
			BitSet bitSet = getGroups().get(key);
			bitSet.set(i);
		}

		BitsRowSubset[] subsets = new BitsRowSubset[getGroupCount()];
		int subsetsIterator = 0;
		for (Entry<String, BitSet> entry : getGroups().entrySet()) {
			subsets[subsetsIterator++] = new BitsRowSubset(entry.getKey(), getGroups().get(entry.getKey()));
		}

		return subsets;
	}
}