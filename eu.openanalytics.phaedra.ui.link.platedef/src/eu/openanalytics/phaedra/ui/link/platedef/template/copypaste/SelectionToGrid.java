package eu.openanalytics.phaedra.ui.link.platedef.template.copypaste;

import java.util.List;
import java.util.stream.Collectors;

import eu.openanalytics.phaedra.base.util.misc.NumberUtils;
import eu.openanalytics.phaedra.link.platedef.template.PlateTemplate;
import eu.openanalytics.phaedra.link.platedef.template.WellTemplate;
import eu.openanalytics.phaedra.ui.link.platedef.template.tab.ITemplateTab;

public class SelectionToGrid {

	public String apply(PlateTemplate template, ITemplateTab tab, List<WellTemplate> currentSelection) {
		List<WellTemplate> orderedSelection = currentSelection.stream()
				.sorted((w1,w2) -> Integer.valueOf(w1.getNr()).compareTo(w2.getNr()))
				.collect(Collectors.toList());
		int[] startPos = NumberUtils.getWellPosition(orderedSelection.get(0).getNr(), template.getColumns());
		int[] endPos = NumberUtils.getWellPosition(orderedSelection.get(orderedSelection.size() - 1).getNr(), template.getColumns());
		
		StringBuilder sb = new StringBuilder();
		for (int r=startPos[0]; r<=endPos[0]; r++) {
			for (int c=startPos[1]; c<=endPos[1]; c++) {
				int wellNr = NumberUtils.getWellNr(r, c, template.getColumns());
				String value = tab.getValue(template.getWells().get(wellNr));
				sb.append(value);
				if (c < endPos[1]) sb.append("\t");
			}
			if (r < endPos[0]) sb.append("\r\n");
		}
		
		return sb.toString();
	}
}
