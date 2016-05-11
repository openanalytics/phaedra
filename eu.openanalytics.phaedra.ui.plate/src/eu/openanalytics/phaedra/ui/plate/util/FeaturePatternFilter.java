package eu.openanalytics.phaedra.ui.plate.util;

import org.eclipse.jface.viewers.Viewer;

import eu.openanalytics.phaedra.base.ui.util.filter.NamePatternFilter;
import eu.openanalytics.phaedra.model.protocol.vo.FeatureGroup;
import eu.openanalytics.phaedra.model.protocol.vo.IFeature;

public class FeaturePatternFilter extends NamePatternFilter {

	public FeaturePatternFilter() {
		super();
	}

	// By default, the Filter uses a ILabelProvider. StyledCellLabelProvider is an IBaseLabelProvider and not a ILabelProvider implementation.
	@Override
	protected boolean isLeafMatch(Viewer viewer, Object element) {
		String text = "";
		if (element instanceof IFeature) {
			IFeature feature = (IFeature) element;
			text = feature.getName();
		}
		if (element instanceof FeatureGroup) {
			text = ((FeatureGroup) element).getName();
		}
		return wordMatches(text);
	}

}
