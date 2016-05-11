package eu.openanalytics.phaedra.ui.link.importer.util;

import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;

public abstract class BaseDropTarget extends DropTargetAdapter {
	
	public void dragEnter(DropTargetEvent event) {
		if (event.detail == DND.DROP_DEFAULT)
			event.detail = DND.DROP_COPY;
	}
	public void dragOperationChanged(DropTargetEvent event) {
		if (event.detail == DND.DROP_DEFAULT)
			event.detail = DND.DROP_COPY;
	}
	public void dropAccept(DropTargetEvent event){
		if (event.detail == DND.DROP_DEFAULT)
			event.detail = DND.DROP_COPY;
	}
	public void drop(DropTargetEvent event) {
		if (event.data == null) {
			event.detail = DND.DROP_NONE;
			return;
		}
		dropImpl(event);
	}
	
	protected abstract void dropImpl(DropTargetEvent event);
}
