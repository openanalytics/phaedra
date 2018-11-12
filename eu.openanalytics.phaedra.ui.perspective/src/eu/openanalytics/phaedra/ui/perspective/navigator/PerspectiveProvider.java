package eu.openanalytics.phaedra.ui.perspective.navigator;

import java.util.List;

import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.ui.navigator.NavigatorContentProvider;
import eu.openanalytics.phaedra.base.ui.navigator.model.Element;
import eu.openanalytics.phaedra.base.ui.navigator.model.Group;
import eu.openanalytics.phaedra.base.ui.navigator.model.IElement;
import eu.openanalytics.phaedra.base.ui.navigator.model.IGroup;
import eu.openanalytics.phaedra.base.ui.navigator.providers.IElementProvider;
import eu.openanalytics.phaedra.ui.perspective.PerspectiveService;
import eu.openanalytics.phaedra.ui.perspective.vo.SavedPerspective;

public class PerspectiveProvider implements IElementProvider {

	public final static String PSP_PREFIX = "PSP_";
	public final static String PSP_GROUP_SUFFIX = "PSPs";
	
	public final static String PSP_GROUP_ALL = "All" + PSP_GROUP_SUFFIX;
	public final static String PSP_GROUP_PRIVATE = "My" + PSP_GROUP_SUFFIX;
	public final static String PSP_GROUP_TEAM = "Team" + PSP_GROUP_SUFFIX;
	public final static String PSP_GROUP_PUBLIC = "Public" + PSP_GROUP_SUFFIX;
	
	@Override
	public IElement[] getChildren(IGroup parent) {
		IElement[] elements = null;
		if (parent == NavigatorContentProvider.ROOT_GROUP) {
			elements = new IElement[1];
			elements[0] = new Group("Perspectives", PSP_GROUP_ALL, parent.getId());
		} else if (parent.getId().equals(PSP_GROUP_ALL)) {
//			elements = new IElement[3];
//			elements[0] = new Group("My Perspectives", PSP_GROUP_PRIVATE, parent.getId());
//			((Group)elements[0]).setImageDescriptor(IconManager.getIconDescriptor("user.png"));
//			elements[1] = new Group("Team Perspectives", PSP_GROUP_TEAM, parent.getId());
//			((Group)elements[1]).setImageDescriptor(IconManager.getIconDescriptor("group.png"));
//			elements[2] = new Group("Public Perspectives", PSP_GROUP_PUBLIC, parent.getId());
//			((Group)elements[2]).setImageDescriptor(IconManager.getIconDescriptor("group.png"));
//		} else if (parent.getId().equals(PSP_GROUP_PRIVATE)) {
			List<SavedPerspective> perspectives = PerspectiveService.getInstance().getPrivatePerspectives();
			elements = createElements(perspectives, parent.getId());
		} else if (parent.getId().equals(PSP_GROUP_TEAM)) {
			List<SavedPerspective> perspectives = PerspectiveService.getInstance().getTeamPerspectives();
			elements = createElements(perspectives, parent.getId());
		} else if (parent.getId().equals(PSP_GROUP_PUBLIC)) {
			List<SavedPerspective> perspectives = PerspectiveService.getInstance().getPublicPerspectives();
			elements = createElements(perspectives, parent.getId());
		}
		return elements;
	}

	private IElement[] createElements(List<SavedPerspective> perspectives, String parentId) {
		IElement[] elements = new IElement[perspectives.size()];
		for (int i = 0; i < perspectives.size(); i++) {
			SavedPerspective p = perspectives.get(i);
			elements[i] = new Element(p.getName(), PSP_PREFIX + p.getId(), parentId, IconManager.getDefaultIconDescriptor(SavedPerspective.class));
			((Element)elements[i]).setData(p);
		}
		return elements;
	}
}
