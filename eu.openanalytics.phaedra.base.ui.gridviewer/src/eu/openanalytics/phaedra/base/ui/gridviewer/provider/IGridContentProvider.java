package eu.openanalytics.phaedra.base.ui.gridviewer.provider;

import org.eclipse.jface.viewers.IStructuredContentProvider;

public interface IGridContentProvider extends IStructuredContentProvider {

	public int getRows(Object inputElement);
	
	public int getColumns(Object inputElement);
	
	public Object getElement(int row, int column);
}
