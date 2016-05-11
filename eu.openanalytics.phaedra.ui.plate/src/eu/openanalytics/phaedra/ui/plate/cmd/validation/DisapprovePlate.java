package eu.openanalytics.phaedra.ui.plate.cmd.validation;

import java.util.List;

import org.eclipse.swt.widgets.Display;

import eu.openanalytics.phaedra.base.security.model.Permissions;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.validation.dialog.DisapprovePlatesDialog;

public class DisapprovePlate extends AbstractPlateValidationCommand {

	@Override
	protected String getRequiredRole() {
		return Permissions.PLATE_CHANGE_APPROVAL;
	}

	@Override
	protected boolean getUserConfirmation(List<Plate> plates) {
		return true;
	}
	
	@Override
	protected void doAction(List<Plate> plates) {
		new DisapprovePlatesDialog(Display.getDefault().getActiveShell(), plates).open();
	}
}
