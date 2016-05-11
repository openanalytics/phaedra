package eu.openanalytics.phaedra.ui.protocol.navigator;

import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.ui.navigator.model.Element;
import eu.openanalytics.phaedra.base.ui.navigator.model.IElement;
import eu.openanalytics.phaedra.base.ui.navigator.model.IGroup;
import eu.openanalytics.phaedra.base.ui.navigator.providers.IElementProvider;

public class ProtocolClassProvider implements IElementProvider {

	@Override
	public IElement[] getChildren(IGroup parent) {
		if (parent.getId().equals("protocols")) {
			IElement[] elements = new IElement[1];
			elements[0] = new Element("All Protocol Classes", "all.protocolclasses", "protocols",
					IconManager.getIconDescriptor("book.png"));
			return elements;
		}
		return null;
	}
}
