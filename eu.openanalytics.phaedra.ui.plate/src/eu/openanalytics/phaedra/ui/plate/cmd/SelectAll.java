package eu.openanalytics.phaedra.ui.plate.cmd;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;

public class SelectAll extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		IWorkbenchPart activePart = PlatformUI.getWorkbench() .getActiveWorkbenchWindow().getActivePage().getActivePart();
		if (activePart == null) return null;

		ISelectionProvider provider = activePart.getSite().getSelectionProvider();
		if (provider == null) return null;

		provider.setSelection(null);

		return null;
	}

}
