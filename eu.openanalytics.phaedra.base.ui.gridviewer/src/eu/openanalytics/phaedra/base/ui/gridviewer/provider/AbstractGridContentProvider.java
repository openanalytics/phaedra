package eu.openanalytics.phaedra.base.ui.gridviewer.provider;

import org.eclipse.jface.viewers.Viewer;

public abstract class AbstractGridContentProvider implements IGridContentProvider {

	@Override
	public Object[] getElements(Object inputElement) {
		// This method is not used by the GridViewer.
		return new String[]{"dummy"};
	}

	@Override
	public void dispose() {
		// Nothing to dispose.
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		// Do nothing.
	}
	
	@Override
	public abstract int getColumns(Object inputElement);
	
	@Override
	public abstract int getRows(Object inputElement);
	
	@Override
	public abstract Object getElement(int row, int column);

}
