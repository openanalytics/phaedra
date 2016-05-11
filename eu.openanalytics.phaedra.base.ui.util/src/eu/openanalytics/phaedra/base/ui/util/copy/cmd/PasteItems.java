package eu.openanalytics.phaedra.base.ui.util.copy.cmd;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.handlers.HandlerUtil;

import eu.openanalytics.phaedra.base.ui.util.copy.CopyPasteSelectionDecorator;
import eu.openanalytics.phaedra.base.ui.util.view.IDecoratedPart;

public class PasteItems extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchPart activePart = HandlerUtil.getActivePart(event);
		if (activePart instanceof IDecoratedPart) {
			IDecoratedPart decoratedPart = (IDecoratedPart) activePart;
			CopyPasteSelectionDecorator decorator = decoratedPart.hasDecorator(CopyPasteSelectionDecorator.class);
			if (decorator == null) throw new UnsupportedOperationException();

			ISelection selection = CopyItems.getCurrentSelectionFromClipboard();
			if (selection != null) decorator.pasteAction(selection);

			long setTime = LocalSelectionTransfer.getTransfer().getSelectionSetTime();
			if (CopyPasteSelectionDecorator.cutDecorator != null && setTime - CopyPasteSelectionDecorator.cutTime < 100) {
				CopyPasteSelectionDecorator.cutDecorator.cutAction(selection);
			}

			CopyPasteSelectionDecorator.cutDecorator = null;
		}

		return null;
	}

}