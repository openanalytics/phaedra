package eu.openanalytics.phaedra.ui.plate.cmd.validation;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.handlers.HandlerUtil;

import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;
import eu.openanalytics.phaedra.model.plate.vo.Plate;

public abstract class AbstractPlateValidationCommand extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		
		// Obtain selected plate(s).
		ISelection selection = (ISelection)HandlerUtil.getCurrentSelection(event);
		List<Plate> plates = SelectionUtils.getObjects(selection, Plate.class);
		if (plates.isEmpty()) {
			// Maybe an experiment selection was made rather than a plate selection.
			List<Experiment> experiments = SelectionUtils.getObjects(selection, Experiment.class);
			plates = new ArrayList<Plate>();
			for (Experiment exp: experiments) {
				plates.addAll(PlateService.getInstance().getPlates(exp));
			}
		}
		if (plates.isEmpty()) return null;
		
		// Security check.
		for (Plate plate: plates) {
			boolean permission = SecurityService.getInstance().checkWithDialog(getRequiredRole(), plate);
			if (!permission) return null;
		}

		// Ask confirmation.
		boolean confirm = getUserConfirmation(plates);
		if (!confirm) return null;
		
		// Perform validation action.
		doAction(plates);
		
		return null;
	}
	
	protected abstract String getRequiredRole();
	
	protected abstract boolean getUserConfirmation(List<Plate> plates);
	
	protected abstract void doAction(List<Plate> plates);
}