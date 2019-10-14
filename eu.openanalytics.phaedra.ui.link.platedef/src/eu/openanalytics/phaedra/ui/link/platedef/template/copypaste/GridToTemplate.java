package eu.openanalytics.phaedra.ui.link.platedef.template.copypaste;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import eu.openanalytics.phaedra.base.util.misc.NumberUtils;
import eu.openanalytics.phaedra.link.platedef.template.PlateTemplate;
import eu.openanalytics.phaedra.link.platedef.template.WellTemplate;
import eu.openanalytics.phaedra.ui.link.platedef.template.tab.ITemplateTab;

public class GridToTemplate {

	public boolean apply(PastedGrid grid, PlateTemplate template, ITemplateTab tab, List<WellTemplate> selectedArea) {
		
		/*
		 * Scenarios:
		 * - Paste matches plate size (selection not relevant)
		 * - Paste is single element, fill selection area
		 * - Paste as much as fits, starting at selection offset
		 */
		
		Collections.sort(selectedArea, (w1, w2) -> Integer.valueOf(w1.getNr()).compareTo(Integer.valueOf(w2.getNr())));
		WellTemplate[] selectedBounds = { selectedArea.get(0), selectedArea.get(selectedArea.size() - 1) };
		int[] offset = NumberUtils.getWellPosition(selectedBounds[0].getNr(), template.getColumns());
		int[] offsetEnd = NumberUtils.getWellPosition(selectedBounds[1].getNr(), template.getColumns());
		
		Function<int[], String> gridValueGetter = null;
		
		if (template.getRows() == grid.getRows() && template.getColumns() == grid.getColumns()) {
			gridValueGetter = (pos) -> grid.get(pos[0], pos[1]);
		} else if (grid.getRows() == 1 && grid.getColumns() == 1) {
			gridValueGetter = (pos) -> {
				if (pos[0] < offset[0] || pos[1] < offset[1]) return null;
				if (pos[0] > offsetEnd[0] || pos[1] > offsetEnd[1]) return null;
				return grid.get(1, 1);
			};
		} else {
			gridValueGetter = (pos) -> {
				if (pos[0] < offset[0] || pos[1] < offset[1]) return null;
				int[] gridOffset = { pos[0] - offset[0] + 1, pos[1] - offset[1] + 1};
				if (gridOffset[0] > grid.getRows() || gridOffset[1] > grid.getColumns()) return null;
				return grid.get(gridOffset[0], gridOffset[1]);
			};
		}
		
		boolean modified = false;
		for (int r=1; r<=template.getRows(); r++) {
			for (int c=1; c<=template.getColumns(); c++) {
				int wellNr = NumberUtils.getWellNr(r, c, template.getColumns());
				WellTemplate well = template.getWells().get(wellNr);
				int[] pos = { r, c };
				String value = gridValueGetter.apply(pos);
				if (value == null) continue;
				modified = modified | tab.applyValue(well, value);
			}
		}
		return modified;
	}
}
