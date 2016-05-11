package eu.openanalytics.phaedra.ui.silo.chart.grouping;

import java.awt.Color;
import java.util.BitSet;
import java.util.Map.Entry;

import org.eclipse.swt.graphics.RGB;

import eu.openanalytics.phaedra.base.ui.charting.v2.data.IDataProvider;
import eu.openanalytics.phaedra.base.ui.charting.v2.grouping.DefaultGroupingStrategy;
import eu.openanalytics.phaedra.base.ui.charting.v2.grouping.DefaultStyleProvider;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;
import eu.openanalytics.phaedra.silo.vo.Silo;
import eu.openanalytics.phaedra.ui.silo.chart.data.SiloDataProvider;
import uk.ac.starlink.topcat.BitsRowSubset;

public class SiloWellTypeGroupingStrategy extends DefaultGroupingStrategy<Silo, Silo> {

	private Color[] colors = new Color[] {};

	public SiloWellTypeGroupingStrategy() {
		setStyleProvider(new DefaultStyleProvider() {
			@Override
			public Color[] getColors() {
				return colors;
			}
		});
	}

	@Override
	public String getName() {
		return "Group by Well Type";
	}

	@Override
	public BitsRowSubset[] groupData(IDataProvider<Silo, Silo> dataProvider) {
		getGroups().clear();

		SiloDataProvider siloDataProvider = (SiloDataProvider) dataProvider;
		String[] wellTypeColumn = siloDataProvider.getWellTypeColumn();

		// No Well ID Column.
		int rowCount = dataProvider.getTotalRowCount();
		if (wellTypeColumn == null) {
			BitSet bitset = new BitSet(rowCount);
			bitset.set(0, bitset.size());
			getGroups().put(DEFAULT_GROUPING_NAME, bitset);
		} else {
			for (int i = 0; i < rowCount; i++) {
				String key = wellTypeColumn[i];
				if (!getGroups().containsKey(key)) {
					BitSet bitSet = new BitSet(rowCount);
					getGroups().put(key, bitSet);
				}
				BitSet bitSet = getGroups().get(key);
				bitSet.set(i);
			}
		}

		BitsRowSubset[] subsets = new BitsRowSubset[getGroupCount()];
		colors = new Color[getGroupCount()];
		int subsetsIterator = 0;
		for (Entry<String, BitSet> entry : getGroups().entrySet()) {
			subsets[subsetsIterator] = new BitsRowSubset(entry.getKey(), getGroups().get(entry.getKey()));
			RGB rgb = ProtocolUtils.getWellTypeRGB(entry.getKey());
			colors[subsetsIterator++] = new Color(rgb.red, rgb.green, rgb.blue);
		}

		return subsets;
	}
}