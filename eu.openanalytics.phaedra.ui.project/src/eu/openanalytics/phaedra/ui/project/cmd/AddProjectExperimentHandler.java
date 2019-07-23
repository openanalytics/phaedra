package eu.openanalytics.phaedra.ui.project.cmd;

import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.statushandlers.StatusManager;

import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;
import eu.openanalytics.phaedra.project.ProjectService;
import eu.openanalytics.phaedra.project.vo.Project;
import eu.openanalytics.phaedra.ui.project.Activator;

public class AddProjectExperimentHandler extends AbstractHandler {


	public AddProjectExperimentHandler() {
	}


	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		return null;
	}

	public static List<Experiment> execute(Project project, List<Experiment> experiments) {
		SecurityService securityService = SecurityService.getInstance();
		if (!securityService.checkPersonalObject(SecurityService.Action.UPDATE, securityService.getCurrentUserName(), project, true, false)) {
			return null;
		}
		
		ProjectService projectService = ProjectService.getInstance();
		try {
			return projectService.addExperiments(project, experiments);
		}
		catch (Exception e) {
			StatusManager.getManager().handle(new Status(IStatus.ERROR, Activator.PLUGIN_ID,
							"Failed to add the experiment(s) to the project.", e),
					StatusManager.SHOW | StatusManager.LOG);
			return null;
		}
	}

}
