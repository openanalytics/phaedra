package eu.openanalytics.phaedra.ui.project.navigator;

import java.util.Arrays;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;

import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.ui.navigator.model.IElement;
import eu.openanalytics.phaedra.project.vo.Project;
import eu.openanalytics.phaedra.ui.project.cmd.CreateProjectHandler;

public class ProjectsHandler extends AbstractProjectElementHandler {


	public ProjectsHandler() {
	}


	@Override
	public boolean matches(IElement element) {
		switch (element.getId()) {
		case ProjectProvider.PROJECTS_ROOT_ID:
//		case ProjectProvider.PRIVATE_PROJECTS_ID:
//		case ProjectProvider.TEAM_PROJECTS_ID:
//		case ProjectProvider.PUBLIC_PROJECTS_ID:
			return true;
		default:
			return false;
		}
	}


	@Override
	public void createContextMenu(final IElement[] elements, IMenuManager mgr) {
		mgr.add(new Action("New Project...", IconManager.getCreateIconDescriptor(Project.class)) {
			@Override
			public void run() {
				CreateProjectHandler.execute(CreateProjectHandler.guessAccessScope(Arrays.asList(elements)));
			}
		});
	}

}
