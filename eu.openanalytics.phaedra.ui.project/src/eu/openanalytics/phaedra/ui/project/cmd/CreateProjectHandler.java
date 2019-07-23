package eu.openanalytics.phaedra.ui.project.cmd;

import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.statushandlers.StatusManager;

import eu.openanalytics.phaedra.base.security.model.AccessScope;
import eu.openanalytics.phaedra.base.ui.navigator.model.IElement;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.project.ProjectService;
import eu.openanalytics.phaedra.project.vo.Project;
import eu.openanalytics.phaedra.ui.project.Activator;
import eu.openanalytics.phaedra.ui.project.navigator.AbstractProjectElementHandler;
import eu.openanalytics.phaedra.ui.project.navigator.ProjectProvider;

public class CreateProjectHandler extends AbstractHandler {


	public CreateProjectHandler() {
	}


	@Override
	public void setEnabled(Object evaluationContext) {
	}


	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		List<IElement> elements = SelectionUtils.getObjects(selection, IElement.class, false);
		
		return execute(guessAccessScope(elements));
	}

	public static Project execute(AccessScope accessScope) {
		ProjectService projectService = ProjectService.getInstance();
		Project project = projectService.createProject(accessScope);
		
		Display display = Display.getCurrent();
		EditProjectDialog dialog = new EditProjectDialog(display.getActiveShell(), project) {
			@Override
			protected void okPressed() {
				try {
					projectService.updateProject(project);
					super.okPressed();
				}
				catch (Exception e) {
					StatusManager.getManager().handle(new Status(IStatus.ERROR, Activator.PLUGIN_ID,
									"Failed to create the project.", e),
							StatusManager.SHOW | StatusManager.LOG | StatusManager.BLOCK);
				}
			}
		};
		if (dialog.open() != Dialog.OK) {
			return null;
		}
		
		AbstractProjectElementHandler.showInNavigator(ProjectProvider.getElementPath(project));
		
		return project;
	}


	public static AccessScope guessAccessScope(List<IElement> elements) {
		AccessScope accessScope = null;
		for (IElement element : elements) {
			AccessScope elementAccessScope = guessAccessScope(element);
			if (elementAccessScope != null) {
				if (accessScope == null) {
					accessScope = elementAccessScope;
					continue;
				}
				else if (accessScope == elementAccessScope) {
					continue;
				}
			}
			return null;
		}
		return accessScope;
	}
	
	public static AccessScope guessAccessScope(IElement element) {
		if (element.getId().startsWith(ProjectProvider.PROJECT_ID_PREFIX)) {
			return guessAccessScopeHint(element.getParentId());
		}
		return guessAccessScopeHint(element.getId());
	}

	private static AccessScope guessAccessScopeHint(String scopeGroupId) {
		if (scopeGroupId != null) {
			switch (scopeGroupId) {
			case ProjectProvider.PRIVATE_PROJECTS_ID:
				return AccessScope.PRIVATE;
			case ProjectProvider.TEAM_PROJECTS_ID:
				return AccessScope.TEAM;
			case ProjectProvider.PUBLIC_PROJECTS_ID:
				return AccessScope.PUBLIC_R;
			}
		}
		return null;
	}

}
