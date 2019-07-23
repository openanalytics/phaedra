package eu.openanalytics.phaedra.ui.project.navigator;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.dnd.DragSourceEvent;

import com.google.common.base.Objects;

import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.base.ui.editor.EditorFactory;
import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.ui.navigator.interaction.BaseElementHandler;
import eu.openanalytics.phaedra.base.ui.navigator.model.IElement;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;
import eu.openanalytics.phaedra.project.vo.Project;
import eu.openanalytics.phaedra.ui.project.cmd.RemoveProjectExperimentHandler;

public class ProjectExperimentHandler extends BaseElementHandler {


	public ProjectExperimentHandler() {
	}


	@Override
	public boolean matches(IElement element) {
		return (element.getId().startsWith(ProjectProvider.EXPERIMENT_ID_PREFIX));
	}


	@Override
	public void handleDoubleClick(IElement element) {
		Experiment experiment = (Experiment)element.getData();
		EditorFactory.getInstance().openEditor(experiment);
	}

	@Override
	public void createContextMenu(final IElement[] elements, IMenuManager mgr) {
		SecurityService securityService = SecurityService.getInstance();
		
		List<Experiment> experiments = new ArrayList<>();
		Project singleProject = null;
		for (IElement element : elements) {
			Experiment experiment = (Experiment)element.getData();
			experiments.add(experiment);
			Project project = (Project)element.getParent().getData();
			if (experiments.size() == 1) {
				singleProject = project;
			} else if (!Objects.equal(project, singleProject)) {
				singleProject = null;
			}
		}
		
		if (experiments.size() == 1) {
			mgr.add(new Action("Open", IconManager.getDefaultIconDescriptor(Experiment.class)) {
				@Override
				public void run() {
					EditorFactory.getInstance().openEditor(experiments.get(0));
				}
			});
			mgr.add(new Separator());
		}
		
		if (singleProject != null && securityService.checkPersonalObject(SecurityService.Action.UPDATE, singleProject)) {
			Project project = singleProject;
			mgr.add(new Action("Remove from Project...", IconManager.getIconDescriptor("delete.png")) {
				@Override
				public void run() {
					RemoveProjectExperimentHandler.execute(project, experiments);
				}
			});
		}
//		mgr.add(new Separator());
//		mgr.add(new Action("Add Experiment...") {
//			@Override
//			public void run() {
//			}
//		});
	}

	@Override
	public void dragStart(IElement[] elements, DragSourceEvent event) {
		Object[] datas = new Object[elements.length];
		for (int i = 0; i < datas.length; i++) {
			datas[i] = elements[i].getData();
		}
		LocalSelectionTransfer transfer = LocalSelectionTransfer.getTransfer();
		transfer.setSelectionSetTime(event.time & 0xFFFF);
		transfer.setSelection(new StructuredSelection(datas));
		event.doit = true;
	}

	@Override
	public void dragSetData(IElement[] elements, DragSourceEvent event) {
		LocalSelectionTransfer transfer = LocalSelectionTransfer.getTransfer();
		event.data = transfer.getSelection();
	}

}
