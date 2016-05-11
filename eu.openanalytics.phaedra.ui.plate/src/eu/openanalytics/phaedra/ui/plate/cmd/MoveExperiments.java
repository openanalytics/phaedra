package eu.openanalytics.phaedra.ui.plate.cmd;

import java.util.List;

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
import eu.openanalytics.phaedra.ui.plate.dialog.MoveExperimentDialog;

public class MoveExperiments extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = (ISelection)HandlerUtil.getCurrentSelection(event);
		List<Experiment> experiments = SelectionUtils.getObjects(selection, Experiment.class);
		if (experiments != null && !experiments.isEmpty()) {
			boolean access = true;
			for (Experiment exp: experiments) {
				access = SecurityService.getInstance().checkWithDialog(Permissions.EXPERIMENT_MOVE, exp);
				if (!access) break;
			}
			if (access) {
				MoveExperimentDialog dialog = new MoveExperimentDialog(
						Display.getCurrent().getActiveShell(), experiments);
				dialog.open();
			}
		}
		return null;
	}
}
