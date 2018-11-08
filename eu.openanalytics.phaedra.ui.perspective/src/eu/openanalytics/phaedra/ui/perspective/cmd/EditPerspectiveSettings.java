package eu.openanalytics.phaedra.ui.perspective.cmd;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.handlers.HandlerUtil;

import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.ui.perspective.dialog.PerspectiveSettingsDialog;
import eu.openanalytics.phaedra.ui.perspective.vo.SavedPerspective;

public class EditPerspectiveSettings extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = (ISelection)HandlerUtil.getCurrentSelection(event);
		SavedPerspective perspective = SelectionUtils.getFirstObject(selection, SavedPerspective.class);
		if (perspective == null) return null;
		execute(perspective);
		return null;
	}
	
	public static void execute(SavedPerspective perspective) {
		new PerspectiveSettingsDialog(Display.getDefault().getActiveShell(), perspective).open();
	}
}
