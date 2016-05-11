package eu.openanalytics.phaedra.base.ui.nattable.misc;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.selection.action.SelectCellAction;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;

/**
 * Convenience action that mimics Excel behaviour regarding right-clicking and selections:
 * <ul>
 * <li>If a region of cells is selected, right-clicking inside that region will open the context menu on that selection</li>
 * <li>Right-clicking outside the selected region will change the selection to that cell, and then open the context menu</li>
 * </ul>
 */
public class PrePopupSelectAction extends SelectCellAction {

	@Override
	public void run(NatTable natTable, MouseEvent event) {
		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		ISelection sel = page.getSelection();
		if (sel == null || sel.isEmpty()) super.run(natTable, event);
		if (sel instanceof StructuredSelection && ((StructuredSelection)sel).size() == 1) super.run(natTable, event);
	}
	
}
