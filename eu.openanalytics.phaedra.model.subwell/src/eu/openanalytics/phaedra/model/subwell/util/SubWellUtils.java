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

/**
 * A collection of utilities related to subwell selections.
 * Since subwell items can be very numerous, they are not represented
 * as individual objects in JFace selections, but rather as groups.
 * 
 * See also {@link SubWellSelection}.
 */
public class SubWellUtils {

	/**
	 * Get a list of subwell items from a JFace selection.
	 * 
	 * @param selection The selection containing zero or more subwell items.
	 * @return A list of all subwell items found in the selection.
	 */
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

	/**
	 * Get a list of subwell items from a list of SubWellSelections.
	 * 
	 * @param swSelections The list of SubWellSelections containing all the subwell items.
	 * @return A list of all subwell items found.
	 */
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

	/**
	 * Get a list of SubWellSelections from a JFace selection.
	 * Individual subwell items will be converted into SubWellSelections
	 * for better performance.
	 * 
	 * @param selection The JFace selection to inspect.
	 * @return A list of SubWellSelections representing all selected subwell items.
	 */
	public static List<SubWellSelection> getSubWellSelections(ISelection selection) {
		/*
		 * Only checks the first item so not all the items have to be looped.
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

	/**
	 * Convert a list of individual subwell items into a list of SubWellSelections
	 * for better performance.
	 * 
	 * @param swItems The list of individual subwell items to convert.
	 * @return A list of SubWellSelections representing all selected subwell items.
	 */
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
