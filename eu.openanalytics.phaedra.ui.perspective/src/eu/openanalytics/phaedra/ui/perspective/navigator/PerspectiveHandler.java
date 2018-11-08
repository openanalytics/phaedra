package eu.openanalytics.phaedra.ui.perspective.navigator;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.IMenuManager;

import eu.openanalytics.phaedra.base.ui.navigator.interaction.BaseElementHandler;
import eu.openanalytics.phaedra.base.ui.navigator.model.IElement;
import eu.openanalytics.phaedra.ui.perspective.cmd.DeletePerspective;
import eu.openanalytics.phaedra.ui.perspective.cmd.EditPerspectiveSettings;
import eu.openanalytics.phaedra.ui.perspective.cmd.OpenPerspective;
import eu.openanalytics.phaedra.ui.perspective.cmd.UpdatePerspective;
import eu.openanalytics.phaedra.ui.perspective.vo.SavedPerspective;

public class PerspectiveHandler extends BaseElementHandler {

	@Override
	public boolean matches(IElement element) {
		String id = element.getId();
		return id.startsWith(PerspectiveProvider.PSP_PREFIX);
	}
	
	@Override
	public void handleDoubleClick(IElement element) {
		if (!element.getId().startsWith(PerspectiveProvider.PSP_PREFIX)) return;
		SavedPerspective p = (SavedPerspective)element.getData();
		OpenPerspective.execute(p);
	}
	
	@Override
	public void createContextMenu(IElement[] elements, IMenuManager mgr) {
		List<SavedPerspective> perspectives = new ArrayList<>();
		for (IElement el: elements) perspectives.add((SavedPerspective)el.getData());
		
		if (perspectives.size() == 1) {
			createAction(mgr, "Open", "arrow_out.png", () -> OpenPerspective.execute(perspectives.get(0)));
			createAction(mgr, "Settings...", "pencil.png", () -> EditPerspectiveSettings.execute(perspectives.get(0)));
			createAction(mgr, "Update", "disk.png", () -> UpdatePerspective.execute(perspectives.get(0)));
		}
		createAction(mgr, "Delete", "delete.png", () -> DeletePerspective.execute(perspectives));
	}
}
