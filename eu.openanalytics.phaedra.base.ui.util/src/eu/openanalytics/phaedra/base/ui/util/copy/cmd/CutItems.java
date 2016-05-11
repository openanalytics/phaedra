package eu.openanalytics.phaedra.base.ui.util.copy.cmd;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.handlers.HandlerUtil;

import eu.openanalytics.phaedra.base.ui.util.copy.CopyPasteSelectionDecorator;
import eu.openanalytics.phaedra.base.ui.util.view.IDecoratedPart;

public class CutItems extends CopyItems {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchPart activePart = HandlerUtil.getActivePart(event);
		if (activePart instanceof IDecoratedPart) {
			IDecoratedPart decoratedPart = (IDecoratedPart) activePart;
			CopyPasteSelectionDecorator decorator = decoratedPart.hasDecorator(CopyPasteSelectionDecorator.class);
			if (decorator == null) throw new UnsupportedOperationException();

			CopyPasteSelectionDecorator.cutDecorator = decorator;
			CopyPasteSelectionDecorator.cutTime = System.currentTimeMillis();
		}

		return super.execute(event);
	}

}