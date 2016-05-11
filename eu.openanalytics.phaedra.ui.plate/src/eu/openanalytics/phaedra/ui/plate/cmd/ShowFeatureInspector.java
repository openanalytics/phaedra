package eu.openanalytics.phaedra.ui.plate.cmd;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.handlers.HandlerUtil;

import eu.openanalytics.phaedra.ui.plate.inspector.feature.FeatureInspector;

public class ShowFeatureInspector extends AbstractHandler {
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {		
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindow(event);
		IWorkbenchPage page = window.getActivePage();
		try {
			page.showView(FeatureInspector.class.getName());
		} catch (PartInitException e) {}
		
		return null;
	}
}
