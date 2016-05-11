package eu.openanalytics.phaedra.ui.plate.cmd;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.base.security.model.Permissions;
import eu.openanalytics.phaedra.base.ui.editor.VOEditorInput;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;
import eu.openanalytics.phaedra.ui.plate.browser.PlateBrowser;
import eu.openanalytics.phaedra.ui.plate.dialog.CreatePlateDialog;

public class CreatePlate extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = (ISelection)HandlerUtil.getCurrentSelection(event);
		
		Experiment experiment = SelectionUtils.getFirstObject(selection, Experiment.class);
		
		if (experiment == null) {
			IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
			IEditorPart editor = page.getActiveEditor();
			if (editor != null && editor instanceof PlateBrowser) {
				VOEditorInput input = (VOEditorInput)editor.getEditorInput();
				experiment = SelectionUtils.getFirstAsClass(input.getValueObjects(), Experiment.class);
			}
		}

		if (experiment != null) {
			boolean access = SecurityService.getInstance().checkWithDialog(Permissions.EXPERIMENT_EDIT, experiment);
			if (access) {
				CreatePlateDialog dialog = new CreatePlateDialog(
						Display.getCurrent().getActiveShell(), experiment);
				dialog.open();
			}
		}
		return null;
	}
}
