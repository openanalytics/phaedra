package eu.openanalytics.phaedra.base.ui.navigator.interaction;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.TransferData;

import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.ui.navigator.model.IElement;

/**
 * Base implementation that matches no elements and does nothing.
 */
public class BaseElementHandler implements IElementHandler {

	@Override
	public boolean matches(IElement element) {
		return false;
	}

	@Override
	public void handleDoubleClick(IElement element) {
		// Do nothing.
	}

	@Override
	public void createContextMenu(IElement[] elements, IMenuManager mgr) {
		if (elements != null && elements.length > 0) createContextMenu(elements[0], mgr);
	}
	
	public void createContextMenu(IElement element, IMenuManager mgr) {
		// Do nothing.
	}

	@Override
	public void dragStart(IElement[] elements, DragSourceEvent event) {
		if (elements != null && elements.length > 0) dragStart(elements[0], event);		
	}

	public void dragStart(IElement element, DragSourceEvent event) {
		// Do nothing.
	}

	@Override
	public void dragSetData(IElement[] elements, DragSourceEvent event) {
		if (elements != null && elements.length > 0) dragSetData(elements[0], event);
	}
	
	public void dragSetData(IElement element, DragSourceEvent event) {
		// Do nothing.
	}

	@Override
	public void dragFinished(IElement[] elements, DragSourceEvent event) {
		if (elements != null && elements.length > 0) dragFinished(elements[0], event);		
	}
	
	public void dragFinished(IElement element, DragSourceEvent event) {
		// Do nothing.
	}
	
	@Override
	public boolean validateDrop(IElement element, int operation, TransferData transferType) {
		return false;
	}
	
	@Override
	public boolean performDrop(IElement element, Object data) {
		return false;
	}

	/*
	 * Non-public
	 * **********
	 */
	
	protected void createAction(IMenuManager mgr, String name, String icon, Runnable runnable) {
		Action action = new Action(name, Action.AS_PUSH_BUTTON) {
			@Override
			public void run() {
				runnable.run();
			}
		};
		action.setImageDescriptor(IconManager.getIconDescriptor(icon));
		mgr.add(action);
	}
}