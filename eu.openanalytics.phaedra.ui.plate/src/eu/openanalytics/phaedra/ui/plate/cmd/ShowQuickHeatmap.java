package eu.openanalytics.phaedra.ui.plate.cmd;

import java.util.UUID;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.ui.plate.grid.QuickHeatmap;

public class ShowQuickHeatmap extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		execute((Plate)null);
		return null;
	}

	public static void execute(Plate plate) {
		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		try {
			// Use random secondary ID to open a new view every time.
			String secondaryId = UUID.randomUUID().toString();
			IViewPart part = page.showView(QuickHeatmap.class.getName(), secondaryId, IWorkbenchPage.VIEW_ACTIVATE);
			if (part instanceof QuickHeatmap && plate != null) {
				((QuickHeatmap)part).setPlate(plate);
			}
		} catch (PartInitException e) {}
	}
}
