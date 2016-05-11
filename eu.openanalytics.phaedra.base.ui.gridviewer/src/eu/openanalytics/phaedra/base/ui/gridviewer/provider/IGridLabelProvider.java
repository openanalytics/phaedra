package eu.openanalytics.phaedra.base.ui.gridviewer.provider;

import org.eclipse.jface.viewers.IBaseLabelProvider;

import eu.openanalytics.phaedra.base.ui.gridviewer.widget.GridCell;

public interface IGridLabelProvider extends IBaseLabelProvider {

	public void update(GridCell cell, Object element);
}
