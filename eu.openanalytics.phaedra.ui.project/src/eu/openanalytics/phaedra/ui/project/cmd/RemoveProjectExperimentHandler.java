package eu.openanalytics.phaedra.ui.project.cmd;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.ISources;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.statushandlers.StatusManager;

import com.google.common.base.Objects;

import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.base.ui.navigator.model.IElement;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;
import eu.openanalytics.phaedra.project.ProjectService;
import eu.openanalytics.phaedra.project.vo.Project;
import eu.openanalytics.phaedra.ui.project.Activator;
import eu.openanalytics.phaedra.ui.project.navigator.ProjectProvider;

public class RemoveProjectExperimentHandler extends AbstractHandler {


	public RemoveProjectExperimentHandler() {
	}


	@Override
	public void setEnabled(Object evaluationContext) {
		if (evaluationContext instanceof IEvaluationContext) {
			Object selection = ((IEvaluationContext) evaluationContext).getVariable(ISources.ACTIVE_CURRENT_SELECTION_NAME);
			if (selection instanceof ISelection) {
				List<IElement> elements = SelectionUtils.getObjects((ISelection)selection, IElement.class, false);
				setBaseEnabled(validateSelection(elements));
				return;
			}
		}
		setBaseEnabled(false);
	}

	private boolean validateSelection(List<IElement> elements) {
		if (elements.isEmpty()) {
			return false;
		}
		Project project = null;
		for (IElement element : elements) {
			if (!element.getId().startsWith(ProjectProvider.EXPERIMENT_ID_PREFIX)) {
				return false;
			}
			Project elementProject = (Project)element.getParent().getData();
			if (elementProject != null) {
				if (project == null) {
					project = elementProject;
					continue;
				}
				else if (project.equals(elementProject)) {
					continue;
				}
			}
			return false;
		}
		return SecurityService.getInstance().checkPersonalObject(SecurityService.Action.UPDATE, project);
	}


	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		List<IElement> elements = SelectionUtils.getObjects(selection, IElement.class, false);
		
		List<Experiment> experiments = new ArrayList<>();
		Project singleProject = null;
		for (IElement element : elements) {
			Experiment experiment = (Experiment)element.getData();
			experiments.add(experiment);
			Project project = (Project)element.getParent().getData();
			if (experiments.size() == 1) {
				singleProject = project;
			} else if (!Objects.equal(project, singleProject)) {
				return null;
			}
		}
		if (singleProject == null) {
			return null;
		}
		
		RemoveProjectExperimentHandler.execute(singleProject, experiments);
		
		return null;
	}

	public static boolean execute(Project project, List<Experiment> experiments) {
		if (experiments.isEmpty()) {
			return true;
		}
		if (!MessageDialog.openConfirm(Display.getCurrent().getActiveShell(),
				"Remove Experiment(s)",
				(experiments.size() == 1) ?
						String.format("Are you sure you want to remove the experiment '%1$s' from the project '%2$s'?", experiments.get(0).getName(), project.getName()):
						String.format("Are you sure you want to remove the %1$s experiments from the project '%2$s'?", experiments.size(), project.getName()) )) {
			return false;
		}
		
		SecurityService securityService = SecurityService.getInstance();
		if (!securityService.checkPersonalObject(SecurityService.Action.UPDATE, securityService.getCurrentUserName(), project, true, false)) {
			return false;
		}
		
		ProjectService projectService = ProjectService.getInstance();
		try {
			projectService.removeExperiments(project, experiments);
			return true;
		}
		catch (Exception e) {
			StatusManager.getManager().handle(new Status(IStatus.ERROR, Activator.PLUGIN_ID,
							"Failed to remove the experiment(s) from the project.", e),
					StatusManager.SHOW | StatusManager.LOG);
			return false;
		}
	}

}
