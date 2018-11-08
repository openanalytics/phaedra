package eu.openanalytics.phaedra.ui.perspective.cmd;

import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.handlers.HandlerUtil;

import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.ui.perspective.Activator;
import eu.openanalytics.phaedra.ui.perspective.PerspectiveService;
import eu.openanalytics.phaedra.ui.perspective.vo.SavedPerspective;


public class DeletePerspective extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = (ISelection)HandlerUtil.getCurrentSelection(event);
		List<SavedPerspective> perspectives = SelectionUtils.getObjects(selection, SavedPerspective.class);
		if (perspectives.isEmpty()) return null;
		execute(perspectives);
		return null;
	}
	
	public static void execute(List<SavedPerspective> perspectives) {
		boolean confirmed = MessageDialog.openConfirm(Display.getCurrent().getActiveShell(), "Delete Saved Perspectives",
				"Are you sure you want to delete the " + perspectives.size() + " selected Saved Perspectives?");
		if (!confirmed) return;
		
		try {
			for (SavedPerspective p: perspectives) PerspectiveService.getInstance().deletePerspective(p);
		} catch (Exception e) {
			IStatus status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Failed to delete Saved Perspective", e);
			ErrorDialog.openError(Display.getCurrent().getActiveShell(), status.getMessage(), status.getMessage(), status);
		}
	}
}
