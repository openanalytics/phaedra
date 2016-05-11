package eu.openanalytics.phaedra.base.ui.util.toolitem;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.TypedListener;


public class DropdownToolItemFactory {

	public static ToolItem createDropdown(ToolBar parent) {
		final ToolItem item = new ToolItem(parent, SWT.DROP_DOWN);
	    DropdownSelectionListener listener = new DropdownSelectionListener(item);
	    item.addSelectionListener(listener);
	    // Make sure the children (MenuItems) hold no more references via their SelectionListeners.
	    // E.g. A reference to BaseXYChart in a SelectionListener would cause a memory leak.
	    item.addDisposeListener(e -> disposeChildListeners(item));
	    return item;
	}

	public static MenuItem createChild(ToolItem parent, String text, int style) {
		DropdownSelectionListener l = getListener(parent);
		if (l != null) return l.add(text, style);
		return null;
	}

	public static MenuItem createChildDivider(ToolItem parent) {
		DropdownSelectionListener l = getListener(parent);
		if (l != null) return new MenuItem(l.getMenu(), SWT.SEPARATOR);
		return null;
	}

	public static void clearChildren(ToolItem parent) {
		DropdownSelectionListener l = getListener(parent);
		if (l != null) l.removeAll();
	}

	public static Menu getMenu(ToolItem dropdown) {
		DropdownSelectionListener l = getListener(dropdown);
		if (l != null) return l.getMenu();
		return null;
	}

	private static void disposeChildListeners(ToolItem parent) {
		clearChildren(parent);
		DropdownSelectionListener l = getListener(parent);
		if (l != null) {
			l.getMenu().dispose();
			parent.removeSelectionListener(l);
		}
	}

	private static DropdownSelectionListener getListener(ToolItem item) {
		if (item == null) return null;

		Listener[] listeners = item.getListeners(SWT.Selection);
		DropdownSelectionListener l = null;
		for (Listener listener: listeners) {
			if (listener instanceof TypedListener) {
				Object eventListener = ((TypedListener)listener).getEventListener();
				if (eventListener instanceof DropdownSelectionListener) {
					l = (DropdownSelectionListener)eventListener;
					break;
				}
			}
		}
		return l;
	}
}
