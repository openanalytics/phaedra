package eu.openanalytics.phaedra.ui.perspective.navigator;

import org.eclipse.jface.action.IMenuManager;

import eu.openanalytics.phaedra.base.ui.navigator.interaction.BaseElementHandler;
import eu.openanalytics.phaedra.base.ui.navigator.model.IElement;
import eu.openanalytics.phaedra.ui.perspective.cmd.SaveNewPerspective;

public class PerspectiveGroupHandler extends BaseElementHandler {

	@Override
	public boolean matches(IElement element) {
		String id = element.getId();
		return id.endsWith(PerspectiveProvider.PSP_GROUP_SUFFIX);
	}
	
	@Override
	public void createContextMenu(IElement element, IMenuManager mgr) {
		createAction(mgr, "Save As New...", "add.png", () -> SaveNewPerspective.execute());
	}
}
