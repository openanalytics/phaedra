package eu.openanalytics.phaedra.ui.protocol.navigator;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.IHandlerService;

import eu.openanalytics.phaedra.base.ui.navigator.interaction.BaseElementHandler;
import eu.openanalytics.phaedra.base.ui.navigator.model.IElement;
import eu.openanalytics.phaedra.ui.protocol.cmd.BrowseProtocolClasses;

public class AllProtocolClassesHandler extends BaseElementHandler {

	@Override
	public boolean matches(IElement element) {
		return (element.getId().equals("all.protocolclasses"));
	}

	@Override
	public void handleDoubleClick(IElement element) {
		IHandlerService handlerService = (IHandlerService) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getService(IHandlerService.class);
		try {
			handlerService.executeCommand(BrowseProtocolClasses.class.getName(), null);
		} catch (Exception e) {
			MessageDialog.openError(Display.getDefault().getActiveShell(),
					"Failed to open Protocol Classes Browser", "Failed to open Protocol Classes Browser: " + e.getMessage());
		}
	}

}