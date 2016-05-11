package eu.openanalytics.phaedra.ui.link.platedef.cmd;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.handlers.HandlerUtil;

import eu.openanalytics.phaedra.ui.link.platedef.template.PlateTemplateBrowser;

public class BrowsePlateTemplates extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindow(event);
		IWorkbenchPage page = window.getActivePage();
		try {
			page.showView(PlateTemplateBrowser.class.getName());
		} catch (PartInitException e) {
			throw new ExecutionException("Failed to open the view " + PlateTemplateBrowser.class.getName(), e);
		}
		return null;
	}

}
