package eu.openanalytics.phaedra.ui.project.navigator;

import java.util.Collections;
import java.util.List;

import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import eu.openanalytics.phaedra.base.ui.editor.EditorFactory;
import eu.openanalytics.phaedra.base.ui.navigator.Navigator;
import eu.openanalytics.phaedra.base.ui.navigator.interaction.BaseElementHandler;
import eu.openanalytics.phaedra.base.ui.navigator.model.IElement;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;
import eu.openanalytics.phaedra.project.ProjectService;
import eu.openanalytics.phaedra.project.vo.Project;

public class AbstractProjectElementHandler extends BaseElementHandler {


	public static void showInNavigator(List<IElement[]> elementPaths) {
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (window == null) {
			return;
		}
		Navigator view = (Navigator)window.getActivePage().findView("eu.openanalytics.phaedra.base.ui.navigator.Navigator");
		if (view == null) {
			return;
		}
		view.getSite().getSelectionProvider().setSelection(new TreeSelection(elementPaths.stream()
				.map((path) -> new TreePath(path))
				.toArray(TreePath[]::new) ));
	}
	
	public static void showInNavigator(IElement[] elementPath) {
		showInNavigator(Collections.singletonList(elementPath));
	}
	


	protected void browseExperiments(Project project) {
		List<Experiment> experiments = ProjectService.getInstance().getExperiments(project);
		if (!experiments.isEmpty()) {
			EditorFactory.getInstance().openEditor(experiments);
		}
	}

}
