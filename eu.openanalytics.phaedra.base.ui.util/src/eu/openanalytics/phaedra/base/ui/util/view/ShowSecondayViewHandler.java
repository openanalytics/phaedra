package eu.openanalytics.phaedra.base.ui.util.view;

import java.util.Map;
import java.util.UUID;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Show a view, using a random, unique secondary id.
 */
public class ShowSecondayViewHandler extends AbstractHandler {
    
	public final Object execute(final ExecutionEvent event)
			throws ExecutionException {
		IWorkbenchWindow window = HandlerUtil
				.getActiveWorkbenchWindowChecked(event);
		// Get the view identifier, if any.
		final Map<?,?> parameters = event.getParameters();
		final Object viewId = parameters.get(IWorkbenchCommandConstants.VIEWS_SHOW_VIEW_PARM_ID);
		final String secondaryId = UUID.randomUUID().toString();
		
		if (viewId == null) {
			  throw new ExecutionException("Cannot show view: no view id given"); //$NON-NLS-1$
		} else {
            try {
				openView((String) viewId, secondaryId, window);
            } catch (PartInitException e) {
                throw new ExecutionException("Part could not be initialized", e); //$NON-NLS-1$
            }
		}

		return null;
	}

	private final void openView(final String viewId, final String secondaryId,
			final IWorkbenchWindow activeWorkbenchWindow) throws PartInitException {

		final IWorkbenchPage activePage = activeWorkbenchWindow.getActivePage();
		if (activePage == null) {
			return;
		}

		activePage.showView(viewId, secondaryId, IWorkbenchPage.VIEW_ACTIVATE);
		
	}
}
