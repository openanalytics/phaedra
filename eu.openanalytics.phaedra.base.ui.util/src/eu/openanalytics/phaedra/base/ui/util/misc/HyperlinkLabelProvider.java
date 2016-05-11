package eu.openanalytics.phaedra.base.ui.util.misc;

import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.TextStyle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

/**
 * A cell label provider that contains a clickable hyperlink.
 * 
 * To use: 
 * <ul>
 * <li>Subclass and instantiate, passing Control (Table or Tree) and column index</li>
 * <li>Override getText() and handleLinkClick(Object o)</li>
 */
public class HyperlinkLabelProvider extends StyledCellLabelProvider {

	private Control control;
	private int hyperlinkColumn;
	
	private final Styler hyperlinkStyler = new Styler() {
		@Override
		public void applyStyles(TextStyle textStyle) {
			textStyle.foreground = Display.getDefault().getSystemColor(SWT.COLOR_BLUE);
			textStyle.underline = true;
		}	
	};

	public HyperlinkLabelProvider(Control control, int hyperlinkColumn) {
		this.control = control;
		this.hyperlinkColumn = hyperlinkColumn;
		
		control.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseUp(MouseEvent e) {
				if (e.button == 1) handleMouseClick(e);
			}
		});
		
		control.addMouseMoveListener(new MouseMoveListener() {
			@Override
			public void mouseMove(MouseEvent e) {
				handleMouseMove(e);
			}
		});	
	}
	
	protected String getText(Object o) {
		return null;
	}
	
	protected Image getImage(Object o) {
		return null;
	}
	
	protected void handleLinkClick(Object o) {
		// Default: do nothing.
	}
	
	protected boolean isHyperlinkEnabled(Object o) {
		return true;
	}
	
	@Override
	public void update(ViewerCell cell) {
		String text = getText(cell.getElement());
		if (text != null) {
			if (isHyperlinkEnabled(cell.getElement())) {
				StyledString styledString = new StyledString();
				styledString.append(text, hyperlinkStyler);
				cell.setText(styledString.getString());
				cell.setStyleRanges(styledString.getStyleRanges());
			} else {
				cell.setText(text);
				cell.setStyleRanges(null);
			}
		} else {
			cell.setText(null);
		}
		Image image = getImage(cell.getElement());
		cell.setImage(image);
		super.update(cell);
	}

	private void handleMouseClick(MouseEvent e) {
		Object o = getItemAt(e.x, e.y);
		if (o == null || !isHyperlinkEnabled(o)) return;
		handleLinkClick(o);
	}
	
	private void handleMouseMove(MouseEvent e) {
		Object o = getItemAt(e.x, e.y);
		if (o == null || getText(o) == null || !isHyperlinkEnabled(o)) {
			control.setCursor(null);
		} else {
			control.setCursor(Display.getDefault().getSystemCursor(SWT.CURSOR_HAND));
		}
	}
	
	private Object getItemAt(int x, int y) {
		if (control instanceof Table) {
			Table table = (Table)control;
			Rectangle clientArea = table.getClientArea();
			Point pt = new Point(x, y);
			int index = table.getTopIndex();
			int itemCount = table.getItemCount();
			while (index < itemCount) {
				boolean visible = false;
				TableItem item = table.getItem(index);
				for (int col = 0; col < table.getColumnCount(); col++) {
					Rectangle rect = item.getBounds(col);
					if (rect.contains(pt) && col == hyperlinkColumn) {
						return item.getData();
					}
					if (!visible && rect.intersects(clientArea)) {
						visible = true;
					}
				}
				if (!visible) return null;
				index++;
			}
		} else if (control instanceof Tree) {
			TreeItem item = getItemAt(control, new Point(x, y));
			if (item != null) return item.getData();
		}
		return null;
	}
	
	private TreeItem getItemAt(Object parent, Point pt) {
		if (parent instanceof Tree) {
			Tree tree = (Tree)parent;
			Rectangle clientArea = tree.getClientArea();
			
			// Find index of first visible item.
			int index = 0;
			TreeItem topItem = tree.getTopItem();
			for (; index<tree.getItemCount(); index++) {
				if (tree.getItem(index) == topItem) break;
			}
			
			int itemCount = tree.getItemCount();
			while (index < itemCount) {
				boolean visible = false;
				TreeItem item = tree.getItem(index);
				for (int col = 0; col < tree.getColumnCount(); col++) {
					Rectangle rect = item.getBounds(col);
					if (rect.contains(pt) && col == hyperlinkColumn) {
						return item;
					}
					if (!visible && rect.intersects(clientArea)) {
						visible = true;
					}
				}
				if (!visible) return null;
				index++;
				
				TreeItem childMatch = getItemAt(item, pt);
				if (childMatch != null) return childMatch;
			}
		} else if (parent instanceof TreeItem) {
			TreeItem parentItem = (TreeItem)parent;
			Rectangle clientArea = parentItem.getParent().getClientArea();
			
			int index = 0;
			int itemCount = parentItem.getItemCount();
			while (index < itemCount) {
				boolean visible = false;
				TreeItem item = parentItem.getItem(index);
				for (int col = 0; col < parentItem.getParent().getColumnCount(); col++) {
					Rectangle rect = item.getBounds(col);
					if (rect.contains(pt) && col == hyperlinkColumn) {
						return item;
					}
					if (!visible && rect.intersects(clientArea)) {
						visible = true;
					}
				}
				if (!visible) return null;
				index++;
				
				TreeItem childMatch = getItemAt(item, pt);
				if (childMatch != null) return childMatch;
			}
		}
		return null;
	}
}
