package eu.openanalytics.phaedra.base.ui.navigator.interaction;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.TransferData;

import eu.openanalytics.phaedra.base.ui.navigator.model.IElement;
import eu.openanalytics.phaedra.base.ui.navigator.providers.IElementProvider;

public class ElementHandlerRegistry implements IElementHandler {

	private List<IElementHandler> handlers;
	
	private static ElementHandlerRegistry instance;
	
	private ElementHandlerRegistry() {
		handlers = new ArrayList<IElementHandler>();
		loadHandlers();
	}
	
	public static ElementHandlerRegistry getInstance() {
		if (instance == null) instance = new ElementHandlerRegistry();
		return instance;
	}
	
	private void loadHandlers() {
		IConfigurationElement[] config = Platform.getExtensionRegistry()
				.getConfigurationElementsFor(IElementHandler.EXT_POINT_ID);
		for (IConfigurationElement el : config) {
			try {
				Object o = el.createExecutableExtension(IElementProvider.ATTR_CLASS);
				if (o instanceof IElementHandler) {
					IElementHandler handler = (IElementHandler)o;
					handlers.add(handler);
				}
			} catch (CoreException e) {
				// Invalid extension.
			}
		}
	}
	
	/*
	 * ***************************************
	 * Implement IElementHandler by delegation
	 * ***************************************
	 */
	
	@Override
	public boolean matches(IElement element) {
		return true;
	}

	@Override
	public void handleDoubleClick(IElement element) {
		for (IElementHandler handler: handlers) {
			if (handler.matches(element)) handler.handleDoubleClick(element);
		}
	}

	@Override
	public void createContextMenu(IElement[] elements, IMenuManager mgr) {
		for (IElementHandler handler: handlers) {
			if (matches(handler, elements)) handler.createContextMenu(elements, mgr);
		}
	}

	@Override
	public void dragStart(IElement[] elements, DragSourceEvent event) {
		for (IElementHandler handler: handlers) {
			if (matches(handler, elements)) handler.dragStart(elements, event);
		}
	}

	@Override
	public void dragSetData(IElement[] elements, DragSourceEvent event) {
		for (IElementHandler handler: handlers) {
			if (matches(handler, elements)) handler.dragSetData(elements, event);
		}
	}

	@Override
	public void dragFinished(IElement[] elements, DragSourceEvent event) {
		for (IElementHandler handler: handlers) {
			if (matches(handler, elements)) handler.dragFinished(elements, event);
		}
	}

	@Override
	public boolean validateDrop(IElement element, int operation, TransferData transferType) {
		boolean valid = false;
		for (IElementHandler handler: handlers) {
			if (handler.matches(element)) {
				valid = handler.validateDrop(element, operation, transferType);
			}
		}
		return valid;
	}

	public boolean performDrop(IElement element, Object data) {
		boolean valid = false;
		for (IElementHandler handler: handlers) {
			if (handler.matches(element)) {
				valid = handler.performDrop(element, data);
			}
		}
		return valid;
	}
	
	private boolean matches(IElementHandler handler, IElement[] elements) {
		if (elements == null || elements.length == 0) return false;
		return handler.matches(elements[0]);
	}
}