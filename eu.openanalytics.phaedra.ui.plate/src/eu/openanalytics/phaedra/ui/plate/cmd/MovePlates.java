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
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.ui.plate.dialog.MovePlatesDialog;

public class MovePlates extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = (ISelection)HandlerUtil.getCurrentSelection(event);
		List<Plate> plates = SelectionUtils.getObjects(selection, Plate.class);
		if (plates != null && !plates.isEmpty()) {
			boolean access = true;
			for (Plate plate: plates) {
				access = SecurityService.getInstance().checkWithDialog(Permissions.PLATE_MOVE, plate);
				if (!access) break;
			}
			if (access) {
				MovePlatesDialog dialog = new MovePlatesDialog(
						Display.getCurrent().getActiveShell(), plates);
				dialog.open();
			}
		}
		return null;
	}
}
