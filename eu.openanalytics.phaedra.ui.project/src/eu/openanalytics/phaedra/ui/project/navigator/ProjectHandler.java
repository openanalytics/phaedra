package eu.openanalytics.phaedra.ui.project.navigator;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.dnd.TransferData;

import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.ui.navigator.model.IElement;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;
import eu.openanalytics.phaedra.project.vo.Project;
import eu.openanalytics.phaedra.ui.project.cmd.AddProjectExperimentHandler;
import eu.openanalytics.phaedra.ui.project.cmd.CreateProjectHandler;
import eu.openanalytics.phaedra.ui.project.cmd.DeleteProjectHandler;
import eu.openanalytics.phaedra.ui.project.cmd.EditProjectHandler;

public class ProjectHandler extends AbstractProjectElementHandler {


	public ProjectHandler() {
	}


	@Override
	public boolean matches(IElement element) {
		return (element.getId().startsWith(ProjectProvider.PROJECT_ID_PREFIX));
	}


	@Override
	public void handleDoubleClick(IElement element) {
		browseExperiments((Project)element.getData());
	}

	@Override
	public void createContextMenu(final IElement[] elements, IMenuManager mgr) {
		List<Project> projects = Arrays.stream(elements)
				.map((element) -> (Project)element.getData())
				.collect(Collectors.toList());
		if (projects.size() == 1) {
			mgr.add(new Action("Browse Experiments", IconManager.getDefaultIconDescriptor(Project.class)) {
				@Override
				public void run() {
					browseExperiments(projects.get(0));
				}
			});
			mgr.add(new Action("Edit Properties...", IconManager.getUpdateIconDescriptor(Project.class)) {
				@Override
				public void run() {
					EditProjectHandler.execute(projects.get(0));
				}
			});
//			if (SecurityService.getInstance().checkPersonalObject(SecurityService.Action.UPDATE, projects.get(0))) {
//				mgr.add(new Action("Add Experiment...") {
//					@Override
//					public void run() {
//					}
//				});
//			}
			if (projects.stream().allMatch((project) -> SecurityService.getInstance().checkPersonalObject(SecurityService.Action.DELETE, project))) {
				mgr.add(new Action("Delete...", IconManager.getIconDescriptor("delete.png")) {
					@Override
					public void run() {
						DeleteProjectHandler.execute(projects.get(0));
					}
				});
			}
		}
		
		mgr.add(new Separator());
		mgr.add(new Action("New Project...", IconManager.getCreateIconDescriptor(Project.class)) {
			@Override
			public void run() {
				CreateProjectHandler.execute(CreateProjectHandler.guessAccessScope(Arrays.asList(elements)));
			}
		});
	}

	@Override
	public boolean validateDrop(IElement element, int operation, TransferData transferType) {
		if (element.getId().startsWith(ProjectProvider.PROJECT_ID_PREFIX)
				&& LocalSelectionTransfer.getTransfer().isSupportedType(transferType)) {
			ISelection data = LocalSelectionTransfer.getTransfer().getSelection();
			if (data instanceof IStructuredSelection) {
				IStructuredSelection structuredSelection = (IStructuredSelection)data;
				for (Iterator<?> iter = structuredSelection.iterator(); iter.hasNext();) {
					Object object = iter.next();
					if (isSupportedProjectContent(object)) {
						// DND.DROP_LINK DND.DROP_COPY
						return true;
					}
				}
			}
		}
		return false;
	}
	
	private boolean isSupportedProjectContent(Object object) {
		return (object instanceof Experiment);
	}
	
	@Override
	public boolean performDrop(IElement element, Object data) {
		if (element.getId().startsWith(ProjectProvider.PROJECT_ID_PREFIX)) {
			Project project = (Project)element.getData();
			if (data instanceof IStructuredSelection) {
				List<Experiment> added = AddProjectExperimentHandler.execute(project,
						SelectionUtils.getObjects((IStructuredSelection) data, Experiment.class, false) );
				if (added != null) {
					if (!added.isEmpty()) {
						showInNavigator(ProjectProvider.getElementPaths(project, added));
					}
					return true;
				}
			}
		}
		return false;
	}

}
