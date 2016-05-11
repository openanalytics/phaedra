package eu.openanalytics.phaedra.ui.plate.cmd;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.handlers.HandlerUtil;

import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.base.security.model.Permissions;
import eu.openanalytics.phaedra.base.ui.editor.EditorFactory;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;

public class BrowsePlates extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = (ISelection)HandlerUtil.getCurrentSelection(event);
		Experiment experiment = SelectionUtils.getFirstObject(selection, Experiment.class);
		if (experiment != null) {
			boolean access = SecurityService.getInstance().checkWithDialog(Permissions.EXPERIMENT_OPEN, experiment);
			if (access) {
				showEditor(experiment);
			}
		}
		return null;
	}
	
	public static void execute(Experiment experiment) {
		new BrowsePlates().showEditor(experiment);
	}
	
	private void showEditor(Experiment experiment) {
		EditorFactory.getInstance().openEditor(experiment);
	}
}
