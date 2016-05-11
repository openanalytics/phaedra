package eu.openanalytics.phaedra.ui.plate.cmd;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.base.security.model.Permissions;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.ui.plate.dialog.EditPlateDialog;

public class EditPlate extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = (ISelection)HandlerUtil.getCurrentSelection(event);
		Plate plate = SelectionUtils.getFirstObject(selection, Plate.class);
		execute(plate);
		return null;
	}

	public static void execute(Plate plate) {
		if (plate == null) return;
		boolean access = SecurityService.getInstance().checkWithDialog(Permissions.PLATE_EDIT, plate);
		if (access) {
			EditPlateDialog dialog = new EditPlateDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), plate);
			dialog.open();
		}
	}
}
