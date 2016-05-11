package eu.openanalytics.phaedra.ui.subwell.cmd;

import java.util.UUID;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.ui.subwell.SubWellDataView;

public class BrowseSubWellData extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = (ISelection)HandlerUtil.getCurrentSelection(event);
		Well well = SelectionUtils.getFirstObject(selection, Well.class);
		if (well != null) {
			showView(well);
		}
		return null;
	}
	
	private void showView(Well well) {
		try {
			IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
			page.showView(SubWellDataView.class.getName(), UUID.randomUUID().toString(), IWorkbenchPage.VIEW_ACTIVATE);
		} catch (PartInitException e) {
			throw new RuntimeException(e);
		}
	}
}