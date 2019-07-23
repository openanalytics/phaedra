package eu.openanalytics.phaedra.ui.project.cmd;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.ISources;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.statushandlers.StatusManager;

import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.project.ProjectService;
import eu.openanalytics.phaedra.project.vo.Project;
import eu.openanalytics.phaedra.ui.project.Activator;

public class EditProjectHandler extends AbstractHandler {


	public EditProjectHandler() {
	}


	@Override
	public void setEnabled(Object evaluationContext) {
		if (evaluationContext instanceof IEvaluationContext) {
			Object selection = ((IEvaluationContext) evaluationContext).getVariable(ISources.ACTIVE_CURRENT_SELECTION_NAME);
			if (selection instanceof ISelection) {
				Project project = SelectionUtils.getSingleObject((ISelection)selection, Project.class, true);
				setBaseEnabled(validateSelection(project));
				return;
			}
		}
		setBaseEnabled(false);
	}

	private boolean validateSelection(Project project) {
		if (project == null) {
			return false;
		}
		return SecurityService.getInstance().checkPersonalObject(SecurityService.Action.UPDATE, project);
	}


	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		Project project = SelectionUtils.getSingleObject(selection, Project.class, true);
		if (project == null) {
			return null;
		}
		
		execute(project);
		return null;
	}

	public static boolean execute(Project project) {
		ProjectService projectService = ProjectService.getInstance();
		Project workingCopy = projectService.getWorkingCopy(project);
		
		Display display = Display.getCurrent();
		EditProjectDialog dialog = new EditProjectDialog(display.getActiveShell(), workingCopy) {
			@Override
			protected void okPressed() {
				try {
					projectService.updateProject(project, workingCopy);
					super.okPressed();
				}
				catch (Exception e) {
					StatusManager.getManager().handle(new Status(IStatus.ERROR, Activator.PLUGIN_ID,
									"Failed to save the changes of project properties.", e),
							StatusManager.SHOW | StatusManager.LOG | StatusManager.BLOCK);
				}
			}
		};
		return (dialog.open() == Dialog.OK);
	}

}
