package eu.openanalytics.phaedra.base.ui.util.toolitem;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.TypedListener;

public class DropdownSelectionListener extends SelectionAdapter {

	private ToolItem dropdown;
	private Menu menu;

	public DropdownSelectionListener(ToolItem dropdown) {
		this.dropdown = dropdown;
		menu = new Menu(dropdown.getParent().getShell());
	}

	public MenuItem add(String item, int style) {
		final MenuItem menuItem = new MenuItem(menu, style);
		menuItem.setText(item);
		menuItem.setData(dropdown);
		return menuItem;
	}

	public void removeAll() {
		for (MenuItem item: menu.getItems()) {
			item.setData(null);

			Listener[] listeners = item.getListeners(SWT.Selection);
			for (Listener listener: listeners) {
				if (listener instanceof TypedListener) {
					Object eventListener = ((TypedListener)listener).getEventListener();
					if (eventListener instanceof SelectionListener) {
						item.removeSelectionListener((SelectionListener)eventListener);
						break;
					}
				}
			}
			
			item.dispose();
		}
	}
	
	public Menu getMenu() {
		return menu;
	}
	
	@Override
	public void widgetSelected(SelectionEvent event) {
		ToolItem item = (ToolItem) event.widget;
		Rectangle rect = item.getBounds();
		Point pt = item.getParent().toDisplay(new Point(rect.x, rect.y));
		menu.setLocation(pt.x, pt.y + rect.height);
		menu.setVisible(true);
	}
}