package eu.openanalytics.phaedra.ui.curve.cmd;

import java.util.UUID;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import eu.openanalytics.phaedra.ui.curve.details.CrcDetailsView;

public class ShowCrcDetails extends AbstractHandler implements IHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		execute();
		return null;
	}
	
	public static void execute() {
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		IWorkbenchPage page = window.getActivePage();
		try {
			String secondaryId = UUID.randomUUID().toString();
			page.showView(CrcDetailsView.class.getName(), secondaryId, IWorkbenchPage.VIEW_ACTIVATE);
		} catch (PartInitException e) {}
	}
}