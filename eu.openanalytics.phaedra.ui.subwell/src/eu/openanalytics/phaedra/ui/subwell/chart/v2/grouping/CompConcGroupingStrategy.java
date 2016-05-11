package eu.openanalytics.phaedra.ui.subwell.chart.v2.grouping;

import java.awt.Color;

import org.eclipse.swt.graphics.RGB;

import eu.openanalytics.phaedra.base.ui.charting.v2.data.IDataProvider;
import eu.openanalytics.phaedra.base.ui.charting.v2.grouping.DefaultStyleProvider;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Compound;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;
import uk.ac.starlink.topcat.BitsRowSubset;

/**
 * Group by compound and concentration.
 *
 * E.g. "OC 1234 @ 1e-7" is one group.
 *
 * Note: control wells are treated as separate groups.
 */
public class CompConcGroupingStrategy extends AbstractSubWellGroupingStrategy {

	private Color[] colors;
	
	public CompConcGroupingStrategy() {
		setStyleProvider(new DefaultStyleProvider() {
			@Override
			public Color[] getColors() {
				return colors;
			}
		});
	}
	
	@Override
	public String getName() {
		return "Group by compound and concentration";
	}

	@Override
	public BitsRowSubset[] groupData(IDataProvider<Well, Well> dataProvider) {
		BitsRowSubset[] subsets = super.groupData(dataProvider);
		updateColors();
		return subsets;
	}
	
	@Override
	protected String getKey(Well well) {
		String compName = "None";
		Compound comp = well.getCompound();
		if (comp != null) compName = comp.getType() + " " + comp.getNumber();
		String conc = "" + well.getCompoundConcentration();
		String key = compName + " @ " + conc;
		if (!PlateUtils.isSample(well)) key = well.getWellType();
		return key;
	}

	private void updateColors() {
		colors = new Color[getGroupCount()];
		Color[] defaultColors = new DefaultStyleProvider().getColors();
		int defaultColorIndex = 3; // Skip the first 3 default colors, which overlap with control colors.

		for (int i = 0; i < getGroupCount(); i++) {
			String groupName = getGroupNames()[i];
			if (!groupName.contains("@") && ProtocolUtils.isControl(groupName)) {
				RGB color = ProtocolUtils.getWellTypeRGB(groupName);
				colors[i] = new Color(color.red, color.green, color.blue);
			} else {
				colors[i] = defaultColors[(defaultColorIndex++) % defaultColors.length];
			}
		}
	}
}
