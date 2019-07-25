package eu.openanalytics.phaedra.ui.plate.cmd.validation;

import java.util.List;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

import eu.openanalytics.phaedra.base.security.model.Permissions;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.validation.ValidationJobHelper;
import eu.openanalytics.phaedra.validation.ValidationService.Action;

public class ResetPlateApproval extends AbstractPlateValidationCommand {

	@Override
	protected Permissions.Operation getRequiredRole() {
		return Permissions.PLATE_CHANGE_APPROVAL;
	}

	@Override
	protected boolean getUserConfirmation(List<Plate> plates) {
		return MessageDialog.openQuestion(
				Display.getDefault().getActiveShell(),
				"Reset Plate(s) Approval",
				"Are you sure you want to reset approval for these " + plates.size() + " plate(s)?");
	}
	
	@Override
	protected void doAction(List<Plate> plates) {
		ValidationJobHelper.doInJob(Action.RESET_PLATE_APPROVAL, "", plates);
	}
}
