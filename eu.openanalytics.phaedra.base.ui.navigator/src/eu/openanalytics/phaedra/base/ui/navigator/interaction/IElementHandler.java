package eu.openanalytics.phaedra.base.ui.navigator.interaction;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.TransferData;

import eu.openanalytics.phaedra.base.ui.navigator.Activator;
import eu.openanalytics.phaedra.base.ui.navigator.model.IElement;

public interface IElementHandler {

	public final static String EXT_POINT_ID = Activator.PLUGIN_ID + ".elementHandler";
	public final static String ATTR_CLASS = "class";
	
	public boolean matches(IElement element);
	
	public void handleDoubleClick(IElement element);
	
	public void createContextMenu(IElement[] elements, IMenuManager mgr);
	
	public void dragStart(IElement[] elements, DragSourceEvent event);

	public void dragSetData(IElement[] elements, DragSourceEvent event);

	public void dragFinished(IElement[] elements, DragSourceEvent event);

	public boolean validateDrop(IElement element, int operation, TransferData transferType);

	public boolean performDrop(IElement element, Object data);
	
}