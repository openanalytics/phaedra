package eu.openanalytics.phaedra.ui.plate.chart.v2.grouping;

import java.awt.Color;
import java.util.BitSet;
import java.util.Map.Entry;

import eu.openanalytics.phaedra.base.ui.charting.v2.data.IDataProvider;
import eu.openanalytics.phaedra.base.ui.charting.v2.grouping.DefaultStyleProvider;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import uk.ac.starlink.topcat.BitsRowSubset;

public class WellStatusGroupingStrategy extends AbstractWellGroupingStrategy {

	private static final String REJECTED = "Rejected";
	private static final String ACCEPTED = "Accepted";

	private Color[] colors = new Color[0];

	public WellStatusGroupingStrategy() {
		setStyleProvider(new DefaultStyleProvider() {
			@Override
			public Color[] getColors() {
				return colors;
			}
		});
	}

	@Override
	public String getName() {
		return "Group by well status";
	}

	@Override
	public BitsRowSubset[] groupData(IDataProvider<Plate, Well> dataProvider) {
		BitsRowSubset[] subsets = super.groupData(dataProvider);

		// Get the actual Well Type colors.
		int subsetsIterator = 0;
		colors = new Color[getGroupCount()];
		for (Entry<String, BitSet> entry : getGroups().entrySet()) {
			if (entry.getKey().equals(ACCEPTED)) colors[subsetsIterator++] = Color.GREEN;
			else colors[subsetsIterator++] = Color.RED;
		}

		return subsets;
	}

	@Override
	protected String getKey(Well well) {
		return well.getStatus() < 0 ? REJECTED : ACCEPTED;
	}

}