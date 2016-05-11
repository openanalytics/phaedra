package eu.openanalytics.phaedra.ui.curve.grid.provider;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.nebula.widgets.nattable.coordinate.PositionCoordinate;
import org.eclipse.nebula.widgets.nattable.data.IRowDataProvider;

import eu.openanalytics.phaedra.base.ui.nattable.selection.ColumnSelectionTransformer;
import eu.openanalytics.phaedra.base.ui.nattable.selection.ISelectionDataColumnAccessor;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.model.plate.vo.Compound;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;

public class CompoundSelectionTransformer extends ColumnSelectionTransformer<Compound> {

	private ISelectionDataColumnAccessor<Compound> accessor;

	public CompoundSelectionTransformer(ISelectionDataColumnAccessor<Compound> accessor) {
		this.accessor = accessor;
	}

	@Override
	public List<Compound> transformIngoingSelection(ISelection selection) {
		// Accepted types: Curve, Compound, Well, Plate. Experiment is too big to handle.
		Set<Compound> compounds = new HashSet<>(SelectionUtils.getObjects(selection, Compound.class));
		if (compounds.isEmpty()) {
			// Check if the selection was a Well selection, if it was, there is no need to check all Plates.
			Well well = SelectionUtils.getFirstObject(selection, Well.class);
			if (well == null) {
				List<Plate> plates = SelectionUtils.getObjects(selection, Plate.class);
				for (Plate plate : plates) compounds.addAll(plate.getCompounds());
			}
		}
		return new ArrayList<>(compounds);
	}

	@Override
	public List<?> transformOutgoingSelection(List<Compound> list, IRowDataProvider<Compound> dataProvider
			, PositionCoordinate[] selectedCellPositions) {

		if (selectedCellPositions != null && selectedCellPositions.length > 0) {
			Set<Object> selection = new HashSet<>();
			for (PositionCoordinate coord: selectedCellPositions) {
				int colIndex = coord.getLayer().getColumnIndexByPosition(coord.columnPosition);
				Compound rowObject = dataProvider.getRowObject(coord.rowPosition);
				Object value = accessor.getSelectionValue(rowObject, colIndex);
				if (value != null) selection.add(value);
			}
			return new ArrayList<>(selection);
		}

		return transformOutgoingSelection(list);
	}


}
