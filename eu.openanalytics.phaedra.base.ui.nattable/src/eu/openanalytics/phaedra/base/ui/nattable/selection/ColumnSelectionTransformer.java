package eu.openanalytics.phaedra.base.ui.nattable.selection;

import java.util.List;

import org.eclipse.nebula.widgets.nattable.coordinate.PositionCoordinate;
import org.eclipse.nebula.widgets.nattable.data.IRowDataProvider;

public abstract class ColumnSelectionTransformer<T> implements ISelectionTransformer<T> {

	@Override
	public List<?> transformOutgoingSelection(List<T> list) {
		return list;
	}

	public abstract List<?> transformOutgoingSelection(List<T> list, IRowDataProvider<T> dataProvider, PositionCoordinate[] selectedCellPositions);

}
