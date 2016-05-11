package eu.openanalytics.phaedra.base.ui.nattable.columnChooser;


import java.util.List;

import org.eclipse.nebula.widgets.nattable.columnChooser.ColumnEntry;
import org.eclipse.nebula.widgets.nattable.columnChooser.ColumnGroupEntry;
import org.eclipse.nebula.widgets.nattable.selection.SelectionLayer.MoveDirectionEnum;


public interface ISelectionTreeListener {

	void itemsSelected(List<ColumnEntry> addedItems);

	void itemsRemoved(List<ColumnEntry> removedItems);

	/**
	 * If columns moved are adjacent to each other, they are grouped together.
	 * @param direction
	 * @param selectedColumnGroupEntries
	 */
	void itemsMoved(MoveDirectionEnum direction, List<ColumnGroupEntry> selectedColumnGroupEntries, List<ColumnEntry> movedColumnEntries, List<List<Integer>> fromPositions, List<Integer> toPositions);

	void itemsExpanded(ColumnGroupEntry columnGroupEntry);

	void itemsCollapsed(ColumnGroupEntry columnGroupEntry);

	void itemsFiltered();

}
