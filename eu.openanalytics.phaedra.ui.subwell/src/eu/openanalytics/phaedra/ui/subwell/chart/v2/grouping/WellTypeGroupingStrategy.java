package eu.openanalytics.phaedra.ui.subwell.chart.v2.grouping;

import java.awt.Color;
import java.util.BitSet;
import java.util.Map.Entry;

import org.eclipse.swt.graphics.RGB;

import eu.openanalytics.phaedra.base.ui.charting.v2.data.IDataProvider;
import eu.openanalytics.phaedra.base.ui.charting.v2.grouping.DefaultStyleProvider;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;
import uk.ac.starlink.topcat.BitsRowSubset;

public class WellTypeGroupingStrategy extends AbstractSubWellGroupingStrategy {

	private Color[] colors = new Color[] {};

	public WellTypeGroupingStrategy() {
		setStyleProvider(new DefaultStyleProvider() {
			@Override
			public Color[] getColors() {
				return colors;
			}
		});
	}

	@Override
	public String getName() {
		return "Group by well type";
	}

	@Override
	public BitsRowSubset[] groupData(IDataProvider<Well, Well> dataProvider) {
		BitsRowSubset[] subsets = super.groupData(dataProvider);

		int subsetsIterator = 0;
		colors = new Color[getGroupCount()];
		for (Entry<String, BitSet> entry : getGroups().entrySet()) {
			RGB rgb = ProtocolUtils.getWellTypeRGB(entry.getKey());
			colors[subsetsIterator++] = new Color(rgb.red, rgb.green, rgb.blue);
		}

		return subsets;
	}

	@Override
	protected String getKey(Well well) {
		// PHA-644
		return ProtocolUtils.getCustomHCLCLabel(well.getWellType());
	}

}