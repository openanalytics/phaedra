package eu.openanalytics.phaedra.ui.plate.cmd;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.handlers.HandlerUtil;

import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.base.security.model.Permissions;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;
import eu.openanalytics.phaedra.ui.plate.dialog.EditExperimentDialog;

public class EditExperiment extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = (ISelection)HandlerUtil.getCurrentSelection(event);
		Experiment experiment = SelectionUtils.getFirstObject(selection, Experiment.class);
		execute(experiment);
		return null;
	}
	
	public static void execute(Experiment experiment) {
		if (experiment == null) return;
		boolean access = SecurityService.getInstance().checkWithDialog(Permissions.EXPERIMENT_EDIT, experiment);
		if (access) {
			EditExperimentDialog dialog = new EditExperimentDialog(Display.getCurrent().getActiveShell(), experiment);
			dialog.open();
		}
	}
}
