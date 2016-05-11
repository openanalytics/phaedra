package eu.openanalytics.phaedra.base.ui.navigator.providers;

import eu.openanalytics.phaedra.base.ui.navigator.NavigatorContentProvider;
import eu.openanalytics.phaedra.base.ui.navigator.model.Element;
import eu.openanalytics.phaedra.base.ui.navigator.model.Group;
import eu.openanalytics.phaedra.base.ui.navigator.model.IElement;
import eu.openanalytics.phaedra.base.ui.navigator.model.IGroup;

public class TestProvider implements IElementProvider {

	@Override
	public IElement[] getChildren(IGroup parent) {
		if (parent == NavigatorContentProvider.ROOT_GROUP) {
			Group g = new Group("My Content", "testGroup", null);
			return new IElement[]{g};
		}
		if (parent.getId().equals("testGroup")) {
			IElement[] elements = new IElement[2];
			elements[0] = new Element("My NEO Plates", "id1", "testGroup");
			elements[1] = new Element("My Tibolims Experiments", "id2", "testGroup");
			return elements;
		}
		return null;
	}
}
