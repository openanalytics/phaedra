package eu.openanalytics.phaedra.ui.plate.cmd;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.handlers.HandlerUtil;

import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.base.security.model.Permissions;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.model.plate.vo.Compound;
import eu.openanalytics.phaedra.ui.plate.dialog.EditCompoundDescriptionDialog;

public class EditCompoundDescription extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = (ISelection)HandlerUtil.getCurrentSelection(event);
		Compound compound = SelectionUtils.getFirstObject(selection, Compound.class);
		if (compound == null && event.getTrigger() instanceof Event) {
			Object data = ((Event)event.getTrigger()).data;
			if (data != null) compound = SelectionUtils.getAsClass(data, Compound.class);
		}
		if (compound != null) {
			boolean access = SecurityService.getInstance().checkWithDialog(Permissions.PLATE_EDIT, compound);
			if (access) {
				EditCompoundDescriptionDialog dialog = new EditCompoundDescriptionDialog(Display.getCurrent().getActiveShell(), compound);
				dialog.open();
			}
		}
		return null;
	}
}
