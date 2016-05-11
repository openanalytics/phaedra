package eu.openanalytics.phaedra.base.ui.gridviewer.advanced;

import java.util.List;

import org.eclipse.jface.viewers.Viewer;

import eu.openanalytics.phaedra.base.ui.gridviewer.provider.AbstractGridContentProvider;

public class ModifyableGridContentProvider extends AbstractGridContentProvider {

	private List<Object> elements;

	private int columns;
	private int rows;

	private boolean fillColumnWise;

	@Override
	public int getColumns(Object inputElement) {
		return columns;
	}

	@Override
	public int getRows(Object inputElement) {
		return rows;
	}

	public void resetGrid(int rows, int columns, boolean fillColumnWise) {
		this.rows = rows;
		this.columns = columns;
		this.fillColumnWise = fillColumnWise;
	}

	public int getSize() {
		return elements != null ? elements.size() : 0;
	}

	@Override
	public Object getElement(int row, int column) {
		int index;
		if (fillColumnWise) {
			index = row * columns + column;
		} else {
			index = column * rows + row;
		}
		if (elements != null && index < elements.size()) {
			return elements.get(index);
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		if (newInput instanceof List) {
			elements = (List<Object>)newInput;
		}
	}

}
