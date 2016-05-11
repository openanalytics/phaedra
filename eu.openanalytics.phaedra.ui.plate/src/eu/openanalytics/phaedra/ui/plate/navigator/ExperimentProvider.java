package eu.openanalytics.phaedra.ui.plate.navigator;

import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.ui.navigator.model.Element;
import eu.openanalytics.phaedra.base.ui.navigator.model.IElement;
import eu.openanalytics.phaedra.base.ui.navigator.model.IGroup;
import eu.openanalytics.phaedra.base.ui.navigator.providers.IElementProvider;

public class ExperimentProvider implements IElementProvider {

	@Override
	public IElement[] getChildren(IGroup parent) {
		if (parent.getId().equals("my.team")) {
			IElement[] elements = new IElement[2];
			elements[0] = new Element("My Experiments", "my.experiments", parent.getId(),
					IconManager.getIconDescriptor("map_edit.png"));
			elements[1] = new Element("Team Experiments", "team.experiments", parent.getId(),
					IconManager.getIconDescriptor("map_edit.png"));
			return elements;
		}
		return null;
	}
}
