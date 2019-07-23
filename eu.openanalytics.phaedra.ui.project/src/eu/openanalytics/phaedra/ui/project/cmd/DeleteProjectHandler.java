package eu.openanalytics.phaedra.ui.project.cmd;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.statushandlers.StatusManager;

import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.project.ProjectService;
import eu.openanalytics.phaedra.project.vo.Project;
import eu.openanalytics.phaedra.ui.project.Activator;

public class DeleteProjectHandler extends AbstractHandler {


	public DeleteProjectHandler() {
	}


	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		Project project = SelectionUtils.getSingleObject(selection, Project.class, false);
		if (project != null) {
			execute(project);
		}
		return null;
	}


	public static boolean execute(Project project) {
		if (!MessageDialog.openConfirm(Display.getCurrent().getActiveShell(),
				"Delete Project",
				String.format("Are you sure you want to delete the project '%1$s'?", project.getName()))) {
			return false;
		}
		
		SecurityService securityService = SecurityService.getInstance();
		if (!securityService.checkPersonalObject(SecurityService.Action.DELETE, securityService.getCurrentUserName(), project, true, false)) {
			return false;
		}
		
		ProjectService projectService = ProjectService.getInstance();
		try {
			projectService.deleteProject(project);
			return true;
		}
		catch (Exception e) {
			StatusManager.getManager().handle(new Status(IStatus.ERROR, Activator.PLUGIN_ID,
							"Failed to add the delete the project.", e),
					StatusManager.SHOW | StatusManager.LOG);
			return false;
		}
	}

}
