package eu.openanalytics.phaedra.base.ui.util.misc;

import java.util.function.Function;

import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.PluginDropAdapter;
import org.eclipse.ui.part.PluginTransfer;

public class DNDSupport {

	/**
	 * Add drag support for ISelections to a viewer.
	 * 
	 * @param viewer The viewer to add drag support to.
	 * @param part The workbench part. If this part is closed, the drag support is disposed.
	 */
	public static void addDragSupport(StructuredViewer viewer, IWorkbenchPart part) {
		Transfer[] types = new Transfer[] { LocalSelectionTransfer.getTransfer() };
		int operations = DND.DROP_COPY | DND.DROP_MOVE;
		
		DragSource dragSource = new DragSource(viewer.getControl(), operations);
		dragSource.setTransfer(types);
		dragSource.addDragListener(new DragSourceAdapter() {
			public void dragSetData(DragSourceEvent event) {
				LocalSelectionTransfer.getTransfer().setSelection(viewer.getSelection());
			}
		});
		
		addDisposer(part, dragSource);
	}
	
	/**
	 * Switch drag support (for ISelections) on and off for a viewer.
	 * 
	 * @param viewer The viewer to toggle.
	 * @param enabled True to enable drag support, false to disable.
	 */
	public static void toggleDragSupport(StructuredViewer viewer, boolean enabled) {
		Object o = viewer.getControl().getData(DND.DRAG_SOURCE_KEY);
		if (o != null) ((DragSource) o).dispose();
		if (enabled) {
			Transfer[] types = new Transfer[] { LocalSelectionTransfer.getTransfer() };
			int operations = DND.DROP_COPY | DND.DROP_MOVE;
			viewer.addDragSupport(operations, types, new DragSourceAdapter() {
				@Override
				public void dragStart(DragSourceEvent event) {
					LocalSelectionTransfer.getTransfer().setSelection(viewer.getSelection());
				}
			});
		}
	}
	
	/**
	 * Add drop support to a viewer.
	 * The drop action should be registered in the org.eclipse.ui.dropActions extension point,
	 * and the drag source must support PluginTransfer.
	 * 
	 * @param viewer The viewer to add drop support to.
	 * @param part The workbench part. If this part is closed, the drop support is disposed.
	 * @param targetObjectProvider If the drop target has no data (e.g. an empty table), provides a target object manually.
	 */
	public static void addDropSupport(StructuredViewer viewer, IWorkbenchPart part, Function<DropTargetEvent, Object> targetObjectProvider) {
		Transfer[] types = new Transfer[] { PluginTransfer.getInstance() };
		int operations = DND.DROP_COPY | DND.DROP_MOVE;
		
		DropTarget dropTarget = new DropTarget(viewer.getControl(), operations);
		dropTarget.setTransfer(types);
		dropTarget.addDropListener(new PluginDropAdapter(viewer) {
			@Override
			public void dragEnter(DropTargetEvent event) {
				for (TransferData type: event.dataTypes) {
					if (PluginTransfer.getInstance().isSupportedType(type)) {
						event.currentDataType = type;
						break;
					}
				}
				super.dragEnter(event);
				if (event.detail == DND.DROP_DEFAULT) event.detail = DND.DROP_COPY;
			}
			@Override
			public void drop(DropTargetEvent event) {
				// Workaround: if the drop is in the blank area (not on a row), provide an object manually
				Object target = getCurrentTarget();
				if (target == null && targetObjectProvider != null) {
					event.item = viewer.getControl();
					event.item.setData(targetObjectProvider.apply(event));
					dragOver(event);
				}
				super.drop(event);
			}
		});
		
		addDisposer(part, dropTarget);
	}
	
	private static void addDisposer(IWorkbenchPart part, Widget itemToDispose) {
		// Workaround for DragSource dispose bug: dispose the DragSource BEFORE the viewer's control is disposed.
		IPartListener2 partListener = new PartAdapter() {
			@Override
			public void partClosed(IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) != part) return;
				part.getSite().getPage().removePartListener(this);
				itemToDispose.dispose();
			}
		};
		part.getSite().getPage().addPartListener(partListener);
	}
}
