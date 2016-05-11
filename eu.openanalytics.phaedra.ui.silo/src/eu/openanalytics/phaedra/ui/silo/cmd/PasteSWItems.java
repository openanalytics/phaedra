package eu.openanalytics.phaedra.ui.silo.cmd;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ISelection;

import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.subwell.SubWellItem;
import eu.openanalytics.phaedra.model.subwell.SubWellSelection;
import eu.openanalytics.phaedra.model.subwell.SubWellService;

public class PasteSWItems extends AbstractPasteCommand<SubWellItem> {

	@Override
	protected boolean asNewGroup() {
		return false;
	}

	@Override
	protected String getObjectName() {
		return "subwell item";
	}

	@Override
	protected List<SubWellItem> getItems(ISelection selection) {
		List<SubWellItem> items = new ArrayList<>();

		// Try 1: cast or adapt directly to SubWellItem
		items = SelectionUtils.getObjects(selection, SubWellItem.class);
		if (!items.isEmpty()) return items;

		// Try 2: construct SubWellItems from SubWellSelections
		List<SubWellSelection> selectionItems = SelectionUtils.getObjects(selection, SubWellSelection.class);
		if (!selectionItems.isEmpty()) {
			for (SubWellSelection sel: selectionItems) {
				for (int i = sel.getIndices().nextSetBit(0); i >= 0; i = sel.getIndices().nextSetBit(i+1)) {
					items.add(new SubWellItem(sel.getWell(), i));
				}
			}
			return items;
		}

		// Try 3: construct SubWellItems from Wells
		List<Well> wells = SelectionUtils.getObjects(selection, Well.class);
		if (!wells.isEmpty()) {
			for (Well well: wells) {
				int swItemCount = SubWellService.getInstance().getNumberOfCells(well);
				for (int i=0; i<swItemCount; i++) {
					items.add(new SubWellItem(well, i));
				}
			}
		}

		return items;
	}
}