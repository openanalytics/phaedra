package eu.openanalytics.phaedra.ui.protocol.navigator;

import eu.openanalytics.phaedra.base.ui.navigator.NavigatorContentProvider;
import eu.openanalytics.phaedra.base.ui.navigator.model.Group;
import eu.openanalytics.phaedra.base.ui.navigator.model.IElement;
import eu.openanalytics.phaedra.base.ui.navigator.model.IGroup;
import eu.openanalytics.phaedra.base.ui.navigator.providers.IElementProvider;

public class PhaedraProvider implements IElementProvider {

	@Override
	public IElement[] getChildren(IGroup parent) {
		if (parent == NavigatorContentProvider.ROOT_GROUP) {
			IElement[] elements = new IElement[2];
			elements[0] = new Group("Protocols", "protocols", null, false, null);
			elements[1] = new Group("My Team", "my.team", null, true, null);
			return elements;
		}
		return null;
	}
}
