package eu.openanalytics.phaedra.ui.silo.chart.grouping;

import java.util.BitSet;
import java.util.Map.Entry;

import eu.openanalytics.phaedra.base.ui.charting.v2.data.IDataProvider;
import eu.openanalytics.phaedra.base.ui.charting.v2.grouping.DefaultGroupingStrategy;
import eu.openanalytics.phaedra.silo.vo.Silo;
import eu.openanalytics.phaedra.ui.silo.chart.data.SiloDataProvider;
import uk.ac.starlink.topcat.BitsRowSubset;

public class SiloFeatureGroupingStrategy extends DefaultGroupingStrategy<Silo, Silo> {

	private String columnName;

	@Override
	public String getName() {
		return "Group by Column";
	}

	@Override
	public BitsRowSubset[] groupData(IDataProvider<Silo, Silo> dataProvider) {
		getGroups().clear();

		int totalRowCount = dataProvider.getTotalRowCount();
		if (columnName == null || columnName.isEmpty()) {
			BitSet bitset = new BitSet(totalRowCount);
			bitset.set(0, bitset.size());
			getGroups().put(DEFAULT_GROUPING_NAME, bitset);
		} else {
			SiloDataProvider siloDataProvider = (SiloDataProvider) dataProvider;

			String[] columnData = siloDataProvider.getColumnAsString(columnName);
			for (int i = 0; i < totalRowCount; i++) {
				if (dataProvider.getCurrentFilter().get(i)) {
					String lbl = columnData[i];
					BitSet bitSet = getGroups().get(lbl);
					if (bitSet == null) {
						bitSet = new BitSet(dataProvider.getTotalRowCount());
						getGroups().put(lbl, bitSet);
					}
					bitSet.set(i, true);
				}
			}
		}

		BitsRowSubset[] subsets = new BitsRowSubset[getGroupCount()];
		int subsetsIterator = 0;
		for (Entry<String, BitSet> entry : getGroups().entrySet()) {
			subsets[subsetsIterator++] = new BitsRowSubset(entry.getKey(), getGroups().get(entry.getKey()));
		}

		return subsets;
	}

	public String getClassificationFeature() {
		return columnName;
	}

	public void setClassificationFeature(String columnName) {
		this.columnName = columnName;
	}

}