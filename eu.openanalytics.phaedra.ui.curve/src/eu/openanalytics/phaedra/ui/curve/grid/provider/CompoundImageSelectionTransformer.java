package eu.openanalytics.phaedra.ui.curve.grid.provider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.nebula.widgets.nattable.coordinate.PositionCoordinate;
import org.eclipse.nebula.widgets.nattable.data.IRowDataProvider;

import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.ui.curve.CompoundWithGrouping;

public class CompoundImageSelectionTransformer extends CompoundSelectionTransformer {

	private CompoundImageContentProvider accessor;

	public CompoundImageSelectionTransformer(CompoundGridInput gridInput, CompoundImageContentProvider accessor) {
		super(gridInput, accessor);
		this.accessor = accessor;
	}

	@Override
	public List<?> transformOutgoingSelection(List<CompoundWithGrouping> list, IRowDataProvider<CompoundWithGrouping> dataProvider,
			PositionCoordinate[] selectedCellPositions) {
		if (selectedCellPositions != null && selectedCellPositions.length > 0) {
			// Make sure the Wells are send in order of Compound, Concentration.
			Arrays.sort(selectedCellPositions, (c1, c2) -> {
				if (c1.rowPosition == c2.rowPosition) {
					return Integer.compare(c1.columnPosition, c2.columnPosition);
				} else {
					return Integer.compare(c1.rowPosition, c2.rowPosition);
				}
			});

			Set<Object> selection = new LinkedHashSet<>();
			for (PositionCoordinate coord: selectedCellPositions) {
				int colIndex = coord.getLayer().getColumnIndexByPosition(coord.columnPosition);
				CompoundWithGrouping rowObject = dataProvider.getRowObject(coord.rowPosition);
				Object value = accessor.getSelectionValue(rowObject, colIndex);
				if (value != null) selection.add(value);
				if (value instanceof Well) {
					List<Well> possibleWells = accessor.getPossibleWells((Well) value);
					selection.addAll(possibleWells);
				}
			}
			return new ArrayList<>(selection);
		}

		return transformOutgoingSelection(list);
	}


}
