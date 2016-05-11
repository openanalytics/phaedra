package eu.openanalytics.phaedra.model.subwell.util;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jface.viewers.ISelection;

import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.subwell.SubWellItem;
import eu.openanalytics.phaedra.model.subwell.SubWellSelection;

public class SubWellUtils {

	public static List<SubWellItem> getSubWellItems(ISelection selection) {
		// Only check the first item so not all the items have to be looped.
		Object o = SelectionUtils.getFirstObject(selection, SubWellItem.class);
		if (o != null) return SelectionUtils.getObjects(selection, SubWellItem.class);

		// Check if the selection has SubWellSelections, if so convert to SubWellItems.
		o = SelectionUtils.getFirstObject(selection, SubWellSelection.class);
		if (o != null) {
			List<SubWellSelection> swSelections = SelectionUtils.getObjects(selection, SubWellSelection.class);
			return getSubWellItems(swSelections);
		}

		return new ArrayList<>();
	}

	public static List<SubWellItem> getSubWellItems(List<SubWellSelection> swSelections) {
		List<SubWellItem> items = new ArrayList<>();

		for (SubWellSelection swSel : swSelections) {
			Well w = swSel.getWell();
			BitSet bs = swSel.getIndices();
			for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i+1)) {
				items.add(new SubWellItem(w, i));
			}
		}

		return items;
	}

	public static List<SubWellSelection> getSubWellSelections(ISelection selection) {
		/*
		 * Only checks the first item so not all the items have to be looped.
		 *
		 * Check if the selection has SubWellItems, if so convert to SubWellSelections.
		 * We check for SubWellItems first because checking SubWellSelection first would adapt all SubWellItems.
		 */
		Object o = SelectionUtils.getFirstObject(selection, SubWellItem.class);
		if (o != null) {
			List<SubWellItem> swItems = SelectionUtils.getObjects(selection, SubWellItem.class);
			return getSubWellSelections(swItems);
		}

		o = SelectionUtils.getFirstObject(selection, SubWellSelection.class);
		if (o != null) return SelectionUtils.getObjects(selection, SubWellSelection.class);

		return new ArrayList<>();
	}

	public static List<SubWellSelection> getSubWellSelections(List<SubWellItem> swItems) {
		Map<Well, SubWellSelection> tempMap = new ConcurrentHashMap<>();
		swItems.parallelStream().forEach(item -> {
			Well well = item.getWell();
			tempMap.putIfAbsent(well, new SubWellSelection(well, new BitSet()));
			tempMap.get(well).getIndices().set(item.getIndex());
		});

		List<SubWellSelection> swSelections = new ArrayList<>(tempMap.values());
		//for (Well w : tempMap.keySet()) {
		//	swSelections.add(tempMap.get(w));
		//}

		return swSelections;
	}

}
