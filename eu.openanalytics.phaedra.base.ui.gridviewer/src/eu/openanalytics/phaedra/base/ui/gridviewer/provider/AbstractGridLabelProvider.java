package eu.openanalytics.phaedra.base.ui.gridviewer.provider;

import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.swt.graphics.Point;

import eu.openanalytics.phaedra.base.ui.gridviewer.widget.GridCell;
import eu.openanalytics.phaedra.base.ui.gridviewer.widget.render.IGridCellRenderer;

public abstract class AbstractGridLabelProvider extends BaseLabelProvider implements IGridLabelProvider {

	public void cellLayoutChanged(Point newSize) {
		// Default: do nothing.
	}
	
	public IGridCellRenderer createCellRenderer() {
		// Default: no renderer.
		return null;
	}
	
	@Override
	public void update(GridCell cell, Object element) {
		// Default: do nothing.
	}
}
