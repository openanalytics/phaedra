package eu.openanalytics.phaedra.base.ui.richtableviewer.column;

import java.util.Comparator;

import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerColumn;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;


public class ColumnViewerSorter<E> extends ViewerComparator {

	private final ColumnViewer viewer;
	private final ViewerColumn column;
	private SelectionListener listener;
	
	private final Comparator<E> comparator;
	
	private int direction = SWT.NONE;
	
	
	public ColumnViewerSorter(final ViewerColumn column, final Comparator<E> comparator) {
		this.viewer = column.getViewer();
		this.column = column;
		this.comparator = comparator;
		
		this.listener = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				int direction = SWT.NONE;
				if (ColumnViewerSorter.this.viewer.getComparator() == ColumnViewerSorter.this) {
					direction = ColumnViewerSorter.this.direction;
				}
				set(toggleDirection(direction));
			}
		};
		if (this.column instanceof TableViewerColumn) {
			((TableViewerColumn)column).getColumn().addSelectionListener(this.listener);
		} else if (this.column instanceof TreeViewerColumn) {
			((TreeViewerColumn)column).getColumn().addSelectionListener(this.listener);
		} else {
			throw new RuntimeException("ViewerColumn type not supported: " + column.getClass());
		}
	}
	
	public void dispose() {
		final SelectionListener listener = this.listener;
		if (listener != null) {
			this.listener = null;
			if (this.column instanceof TableViewerColumn) {
				((TableViewerColumn)column).getColumn().removeSelectionListener(listener);
			} else if (this.column instanceof TreeViewerColumn) {
				((TreeViewerColumn)column).getColumn().removeSelectionListener(listener);
			}
		}
		if (this.viewer.getComparator() == this) {
			set(SWT.NONE);
		}
	}
	
	
	public Comparator<E> getColumnComparator() {
		return this.comparator;
	}
	
	
	private int checkDirection(final int direction) {
		switch (direction) {
		case SWT.UP:
			return SWT.UP;
		case SWT.DOWN:
			return SWT.DOWN;
		default:
			return SWT.NONE;
		}
	}
	
	private int toggleDirection(final int direction) {
		switch (direction) {
		case SWT.UP:
			return SWT.DOWN;
		case SWT.DOWN:
			return SWT.NONE;
		default:
			return SWT.UP;
		}
	}
	
	private int getOrder(final int direction) {
		switch (direction) {
		case SWT.UP:
			return 1;
		case SWT.DOWN:
			return -1;
		default:
			return 0;
		}
	}
	
	private void set(final int direction) {
		this.direction = direction;
		if (this.column instanceof TableViewerColumn) {
			final TableColumn tableColumn = ((TableViewerColumn)this.column).getColumn();
			final Table table = tableColumn.getParent();
			table.setSortDirection(direction);
			table.setSortColumn((direction != SWT.NONE) ? tableColumn : null);
		}
		else if (this.column instanceof TreeViewerColumn) {
			final TreeColumn treeColumn = ((TreeViewerColumn)this.column).getColumn();
			final Tree tree = treeColumn.getParent();
			tree.setSortDirection(direction);
			tree.setSortColumn((direction != SWT.NONE) ? treeColumn : null);
		}
		final ViewerComparator comparator = (direction != SWT.NONE) ? this : null;
		if (this.viewer.getComparator() != comparator) {
			this.viewer.setComparator(comparator);
		}
		else {
			this.viewer.refresh();
		}
	}
	
	
	public void setSorter(final int direction) {
		set(checkDirection(direction));
	}
	
	@Override
	public int compare(Viewer viewer, Object e1, Object e2) {
		return getOrder(this.direction) * doCompare(viewer, e1, e2);
	}
	
	@SuppressWarnings("unchecked")
	protected int doCompare(Viewer viewer, Object e1, Object e2) {
		return comparator.compare((E)e1, (E)e2);
	}
	
}
