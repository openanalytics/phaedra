package eu.openanalytics.phaedra.ui.protocol.navigator;

import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.ui.navigator.model.Element;
import eu.openanalytics.phaedra.base.ui.navigator.model.IElement;
import eu.openanalytics.phaedra.base.ui.navigator.model.IGroup;
import eu.openanalytics.phaedra.base.ui.navigator.providers.IElementProvider;

public class ProtocolProvider implements IElementProvider {

	@Override
	public IElement[] getChildren(IGroup parent) {
		if (parent.getId().equals("protocols")) {
			IElement[] elements = new IElement[1];
			elements[0] = new Element("All Protocols", "all.protocols", "protocols",
					IconManager.getIconDescriptor("book_g.png"));
			return elements;
		} else if (parent.getId().equals("my.team")) {
			IElement[] elements = new IElement[1];
			elements[0] = new Element("Team Protocols", "team.protocols", "my.team",
					IconManager.getIconDescriptor("book_g_key.png"));
			return elements;
		}
		return null;
	}
}
