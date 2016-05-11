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
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.ui.plate.dialog.EditWellDescriptionDialog;

public class EditWellDescription extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = (ISelection)HandlerUtil.getCurrentSelection(event);
		Well well = SelectionUtils.getFirstObject(selection, Well.class);
		if (well != null) {
			boolean access = SecurityService.getInstance().checkWithDialog(Permissions.PLATE_EDIT, well);
			if (access) {
				EditWellDescriptionDialog dialog = new EditWellDescriptionDialog(Display.getCurrent().getActiveShell(), well);
				dialog.open();
			}
		}
		return null;
	}
}
