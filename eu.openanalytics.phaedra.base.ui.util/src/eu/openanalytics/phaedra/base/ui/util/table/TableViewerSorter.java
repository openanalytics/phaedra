package eu.openanalytics.phaedra.base.ui.util.table;

import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;

public abstract class TableViewerSorter extends ViewerComparator {
	
	private int direction = SWT.NONE;
	
	private TableViewerColumn column;
	
	private ColumnViewer viewer;
	
	public TableViewerSorter(ColumnViewer viewer, TableViewerColumn column) {
		this.column = column;
		this.viewer = viewer;
		this.column.getColumn().addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {
				if( TableViewerSorter.this.viewer.getComparator() != null ) {
					if( TableViewerSorter.this.viewer.getComparator() == TableViewerSorter.this ) {
						int tdirection = TableViewerSorter.this.direction;
						
						if (tdirection == SWT.UP ) {
							setSorter(TableViewerSorter.this, SWT.DOWN);
						} else if (tdirection == SWT.DOWN ) {
							setSorter(TableViewerSorter.this, SWT.NONE);
						} else if (tdirection == SWT.NONE ) {
							setSorter(TableViewerSorter.this, SWT.UP);
						}
					} else {
						setSorter(TableViewerSorter.this, SWT.UP);
					}
				} else {
					setSorter(TableViewerSorter.this, SWT.UP);
				}
			}
		});
	}
	
	public void setSorter(TableViewerSorter sorter, int direction) {
		sorter.direction = direction;
		if( direction == SWT.NONE ) {
			column.getColumn().getParent().setSortDirection(SWT.NONE);
			column.getColumn().getParent().setSortColumn(null);
			viewer.setComparator(null);
		} else {
			column.getColumn().getParent().setSortColumn(column.getColumn());
			
			if( direction == SWT.UP ) {
				column.getColumn().getParent().setSortDirection(SWT.UP);
			} else if ( direction == SWT.DOWN ) {
				column.getColumn().getParent().setSortDirection(SWT.DOWN);
			}
			
			if( viewer.getComparator() == sorter ) {
				viewer.refresh();
			} else {
				viewer.setComparator(sorter);
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
	
	protected abstract int doCompare(Viewer viewer, Object e1, Object e2);
}
