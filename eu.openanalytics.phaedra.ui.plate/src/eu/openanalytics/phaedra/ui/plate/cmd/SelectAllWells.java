package eu.openanalytics.phaedra.ui.plate.cmd;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;

import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.model.plate.vo.Well;

public class SelectAllWells extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		// If the current selection of the active part contains a well, select all other wells of that plate.
		
		IWorkbenchPart activePart = PlatformUI.getWorkbench() .getActiveWorkbenchWindow().getActivePage().getActivePart();
		if (activePart == null) return null;
		
		ISelectionProvider provider = activePart.getSite().getSelectionProvider();
		if (provider == null) return null;

		ISelection currentSelection = activePart.getSite().getPage().getSelection();
		Well well = SelectionUtils.getFirstObject(currentSelection, Well.class);
		if (well == null) return null;
		
		StructuredSelection sel = new StructuredSelection(well.getPlate().getWells());
		provider.setSelection(sel);
		
		return null;
	}
}
