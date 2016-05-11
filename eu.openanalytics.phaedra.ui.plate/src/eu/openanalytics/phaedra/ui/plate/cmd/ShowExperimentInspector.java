package eu.openanalytics.phaedra.ui.plate.cmd;

import java.util.UUID;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import eu.openanalytics.phaedra.ui.plate.inspector.experiment.ExperimentInspector;

public class ShowExperimentInspector extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		try {
			// Use random secondary ID to open a new view every time.
			String secondaryId = UUID.randomUUID().toString();
			page.showView(ExperimentInspector.class.getName(), secondaryId, IWorkbenchPage.VIEW_ACTIVATE);
		} catch (PartInitException e) {}
		return null;
	}
}
