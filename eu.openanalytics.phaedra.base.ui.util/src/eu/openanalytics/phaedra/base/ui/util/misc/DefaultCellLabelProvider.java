package eu.openanalytics.phaedra.base.ui.util.misc;

import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ViewerCell;

public class DefaultCellLabelProvider extends CellLabelProvider {
	
	public String getText(Object element) {
		return "";
	}
	
	@Override
	public void update(ViewerCell cell) {
		cell.setText(getText(cell.getElement()));
	}
	
}