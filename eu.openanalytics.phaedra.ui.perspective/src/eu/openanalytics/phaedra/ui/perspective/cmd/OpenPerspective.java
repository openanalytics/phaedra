package eu.openanalytics.phaedra.ui.perspective.cmd;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.handlers.HandlerUtil;

import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.ui.perspective.Activator;
import eu.openanalytics.phaedra.ui.perspective.PerspectiveService;
import eu.openanalytics.phaedra.ui.perspective.vo.SavedPerspective;

public class OpenPerspective extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = (ISelection)HandlerUtil.getCurrentSelection(event);
		SavedPerspective perspective = SelectionUtils.getFirstObject(selection, SavedPerspective.class);
		if (perspective == null) return null;
		execute(perspective);
		return null;
	}
	
	public static void execute(SavedPerspective perspective) {
		try {
			PerspectiveService.getInstance().openPerspective(perspective);
		} catch (Exception e) {
			IStatus status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Failed to open Saved Perspective", e);
			ErrorDialog.openError(Display.getCurrent().getActiveShell(), status.getMessage(), status.getMessage(), status);
		}
	}
}
