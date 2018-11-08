package eu.openanalytics.phaedra.ui.perspective.cmd;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;

import eu.openanalytics.phaedra.ui.perspective.Activator;
import eu.openanalytics.phaedra.ui.perspective.PerspectiveService;
import eu.openanalytics.phaedra.ui.perspective.vo.SavedPerspective;

public class SaveNewPerspective extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		execute();
		return null;
	}

	public static void execute() {
		InputDialog dialog = new InputDialog(Display.getCurrent().getActiveShell(), "New Saved Perspective", 
				"Please enter a name for the new Saved Perspective", "New Saved Perspective", null);
		if (dialog.open() == Window.CANCEL) return;
		String name = dialog.getValue();
		
		if (name == null || name.isEmpty()) {
			MessageDialog.openError(Display.getCurrent().getActiveShell(), "Invalid name", "'" + name + "' is not a valid name for a Saved Perspective");
			return;
		}
		
		try {
			SavedPerspective p = PerspectiveService.getInstance().createPerspective(name);
			PerspectiveService.getInstance().savePerspectiveLayout(p);
		} catch (Exception e) {
			IStatus status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Failed to create Saved Perspective", e);
			ErrorDialog.openError(Display.getCurrent().getActiveShell(), status.getMessage(), status.getMessage(), status);
		}
	}
}
