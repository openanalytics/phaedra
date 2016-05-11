package eu.openanalytics.phaedra.ui.plate.cmd;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

import eu.openanalytics.phaedra.base.db.IValueObject;
import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.base.security.model.Permissions;
import eu.openanalytics.phaedra.base.ui.editor.VOEditorInput;
import eu.openanalytics.phaedra.base.util.CollectionUtils;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.model.protocol.vo.Protocol;
import eu.openanalytics.phaedra.ui.plate.browser.ExperimentBrowser;
import eu.openanalytics.phaedra.ui.plate.dialog.CreateExperimentDialog;

public class CreateExperiment extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = (ISelection)HandlerUtil.getCurrentSelection(event);
		
		List<Protocol> protocols = SelectionUtils.getObjects(selection, Protocol.class);
		
		if (protocols.isEmpty()) {
			IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
			IEditorPart editor = page.getActiveEditor();
			if (editor != null && editor instanceof ExperimentBrowser) {
				VOEditorInput input = (VOEditorInput)editor.getEditorInput();
				for (IValueObject vo: input.getValueObjects()) {
					Protocol p = SelectionUtils.getAsClass(vo, Protocol.class);
					if (p != null) CollectionUtils.addUnique(protocols, p);
				}
			}
		}
		
		List<Protocol> accessibleProtocols = new ArrayList<Protocol>();
		for (Protocol p: protocols) {
			boolean access = SecurityService.getInstance().check(Permissions.EXPERIMENT_CREATE, p);
			if (access) accessibleProtocols.add(p);
		}
		
		if (accessibleProtocols.isEmpty()) {
			// Check the failed protocol, but this time with an Access Denied dialog.
			SecurityService.getInstance().checkWithDialog(Permissions.EXPERIMENT_CREATE, protocols.get(0));
		} else {
			CreateExperimentDialog dialog = new CreateExperimentDialog(
					Display.getCurrent().getActiveShell(), protocols);
			dialog.open();
		}
		
		return null;
	}
}
