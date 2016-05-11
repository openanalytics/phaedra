package eu.openanalytics.phaedra.ui.curve.grid;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

public class DropDown {

	private ToolItem toolItem;
	private Menu popupMenu;
	
	private SelectionListener itemSelectionListener;
	
	public DropDown(final ToolBar parent, String name, Image image) {
		toolItem = new ToolItem(parent, SWT.DROP_DOWN);
		if (name != null) {
			toolItem.setText(name);			
		}
		if (image != null) {
			toolItem.setImage(image);
		}
		
		popupMenu = new Menu(parent.getShell(), SWT.POP_UP);
		
		toolItem.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				Rectangle rect = toolItem.getBounds();
				Point pt = new Point(rect.x, rect.y + rect.height);
				pt = parent.toDisplay(pt);
				popupMenu.setLocation(pt.x, pt.y);
				popupMenu.setVisible(true);
			}
		});
		
//		toolItem.addSelectionListener(new SelectionAdapter() {
//			@Override
//			public void widgetSelected(SelectionEvent e) {
//				if (e.detail == 0 && itemSelectionListener != null) {
//					// Find the currently selected item.
//					int selectionIndex = -1;
//					for (int i=0; i<popupMenu.getItemCount(); i++) {
//						MenuItem item = popupMenu.getItem(i);
//						if (item.getSelection()) {
//							selectionIndex = i;
//							break;
//						}
//					}
//					// Deselect it.
//					if (selectionIndex != -1) {
//						popupMenu.getItem(selectionIndex).setSelection(false);
//
//						// Send a (de)selection event.
//						if (itemSelectionListener != null) {
//							Event baseEvent = new Event();
//							baseEvent.widget = popupMenu.getItem(selectionIndex);
//							SelectionEvent event = new SelectionEvent(baseEvent);
//							itemSelectionListener.widgetSelected(event);
//						}
//					}
//					
//					// Find the next item in the list.
//					if (selectionIndex == popupMenu.getItemCount()-1) {
//						selectionIndex = 0;
//					} else {
//						selectionIndex++;
//					}
//					// Select the next item.
//					popupMenu.getItem(selectionIndex).setSelection(true);
//					if (itemSelectionListener != null) {
//						Event baseEvent = new Event();
//						baseEvent.widget = popupMenu.getItem(selectionIndex);
//						SelectionEvent event = new SelectionEvent(baseEvent);
//						itemSelectionListener.widgetSelected(event);
//					}
//				}
//			}
//		});
	}
	
	public ToolItem getToolItem() {
		return toolItem;
	}
	
	public MenuItem getSelectedItem() {
		for (MenuItem item: popupMenu.getItems()) {
			if (item.getSelection()) return item;
		}
		return null;
	}
	
	public void setTooltipText(String text) {
		toolItem.setToolTipText(text);
	}
	
	public void setItemSelectionListener(SelectionListener itemSelectionListener) {
		this.itemSelectionListener = itemSelectionListener;
	}
	
	public MenuItem addRadioItem(String name, Image image, Object data) {
		MenuItem item = new MenuItem(popupMenu, SWT.RADIO);
		item.setText(name);
		item.setData(data);
		if (image != null) {
			item.setImage(image);
		}
		if (popupMenu.getItemCount() == 1) {
			item.setSelection(true);
		}
		if (itemSelectionListener != null) {
			item.addSelectionListener(itemSelectionListener);
		}
		return item;
	}
	
	public void clearItems() {
		for (MenuItem item: popupMenu.getItems()) {
			if (itemSelectionListener != null) {
				item.removeSelectionListener(itemSelectionListener);
			}
			item.dispose();
		}
	}
	
	public void select(int index, boolean triggerSelectionEvent) {
		if (index >= popupMenu.getItemCount()) return; 
		for (int i=0; i<popupMenu.getItemCount(); i++) {
			MenuItem item = popupMenu.getItem(i);
			if (i == index) {
				item.setSelection(true);
				if (triggerSelectionEvent && itemSelectionListener != null) {
					Event e = new Event();
					e.widget = item;
					SelectionEvent event = new SelectionEvent(e);
					itemSelectionListener.widgetSelected(event);
				}
			} else {
				item.setSelection(false);
			}
		}
	}
}
