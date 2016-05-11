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
import org.eclipse.swt.widgets.Tree;

public class ColumnViewerSorter<E> extends ViewerComparator {

	private int direction = SWT.NONE;
	
	private ColumnViewer viewer;
	private ViewerColumn column;
	private Comparator<E> comparator;
		
	public ColumnViewerSorter(ColumnViewer viewer, ViewerColumn column, Comparator<E> comparator) {
		this.column = column;
		this.viewer = viewer;
		this.comparator = comparator;
		
		SelectionListener listener = new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if( ColumnViewerSorter.this.viewer.getComparator() != null ) {
					if( ColumnViewerSorter.this.viewer.getComparator() == ColumnViewerSorter.this ) {
						int tdirection = ColumnViewerSorter.this.direction;
						
						if (tdirection == SWT.UP ) {
							setSorter(SWT.DOWN);
						} else if (tdirection == SWT.DOWN ) {
							setSorter(SWT.NONE);
						} else if (tdirection == SWT.NONE ) {
							setSorter(SWT.UP);
						}
					} else {
						setSorter(SWT.UP);
					}
				} else {
					setSorter(SWT.UP);
				}
			}
		};
		
		if (column instanceof TableViewerColumn) {
			((TableViewerColumn)column).getColumn().addSelectionListener(listener);
		} else if (column instanceof TreeViewerColumn) {
			((TreeViewerColumn)column).getColumn().addSelectionListener(listener);
		}
		else throw new RuntimeException("ViewerColumn type not supported: " + column.getClass());
	}
	
	public void setSorter(int direction) {
		Table table = null;
		Tree tree = null;
		if (column instanceof TableViewerColumn) table = ((TableViewerColumn)column).getColumn().getParent();
		else if (column instanceof TreeViewerColumn) tree = ((TreeViewerColumn)column).getColumn().getParent();
		
		if (table != null) {
			this.direction = direction;
			if( direction == SWT.NONE ) {
				table.setSortDirection(SWT.NONE);
				table.setSortColumn(null);
				viewer.setComparator(null);
			} else {
				table.setSortColumn(((TableViewerColumn)column).getColumn());
				
				if( direction == SWT.UP ) {
					table.setSortDirection(SWT.UP);
				} else if ( direction == SWT.DOWN ) {
					table.setSortDirection(SWT.DOWN);
				}
				
				if (this == viewer.getComparator()) {
					viewer.refresh();
				} else {
					viewer.setComparator(this);
				}
			}
		} else if (tree != null) {
			this.direction = direction;
			if (direction == SWT.NONE ) {
				tree.setSortDirection(SWT.NONE);
				tree.setSortColumn(null);
				viewer.setComparator(null);
			} else {
				tree.setSortColumn(((TreeViewerColumn)column).getColumn());
				
				if (direction == SWT.UP ) {
					tree.setSortDirection(SWT.UP);
				} else if ( direction == SWT.DOWN ) {
					tree.setSortDirection(SWT.DOWN);
				}
				
				if (this == viewer.getComparator()) {
					viewer.refresh();
				} else {
					viewer.setComparator(this);
				}
			}
		}
	}
	
	public int compare(Viewer viewer, Object e1, Object e2) {
		int order;
		if( direction == SWT.UP ) {
			order = 1;
		} else if( direction == SWT.DOWN ) {
			order = -1;
		} else {
			order = 0;
		}
		return order * doCompare(viewer, e1, e2);
	}
	
	@SuppressWarnings("unchecked")
	protected int doCompare(Viewer viewer, Object e1, Object e2) {
		return comparator.compare((E)e1, (E)e2);
	}
}
