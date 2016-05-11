package eu.openanalytics.phaedra.base.ui.nattable.command;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.nebula.widgets.nattable.command.AbstractLayerCommandHandler;
import org.eclipse.nebula.widgets.nattable.coordinate.Range;
import org.eclipse.nebula.widgets.nattable.layer.DataLayer;
import org.eclipse.nebula.widgets.nattable.resize.command.MultiColumnResizeCommand;
import org.eclipse.nebula.widgets.nattable.resize.command.MultiColumnResizeCommandHandler;
import org.eclipse.nebula.widgets.nattable.resize.event.ColumnResizeEvent;

/**
 * <p>Based on {@link MultiColumnResizeCommandHandler}</p>
 *
 * <p>Sends only one event with a range that spans all the changed columns, this might include columns from which the size was not changed.</p>
 */
public class FastMultiColumnResizeCommandHandler extends AbstractLayerCommandHandler<MultiColumnResizeCommand> {

	private final DataLayer dataLayer;

	public FastMultiColumnResizeCommandHandler(DataLayer dataLayer) {
		this.dataLayer = dataLayer;
	}

	@Override
	public Class<MultiColumnResizeCommand> getCommandClass() {
		return MultiColumnResizeCommand.class;
	}

	@Override
	protected boolean doCommand(MultiColumnResizeCommand command) {
		List<Integer> columnPositions = new ArrayList<Integer>();

		for (int columnPosition : command.getColumnPositions()) {
			columnPositions.add(columnPosition);
			dataLayer.setColumnWidthByPosition(columnPosition, command.getColumnWidth(columnPosition), false);
		}

		// Changed: Only send one event with a range that contains all columns (even some that were not changed).
		int start = Integer.MAX_VALUE;
		int end = 0;
		for (Integer columnPosition : columnPositions) {
			start = Math.min(start, columnPosition);
			end = Math.max(end, columnPosition);
		}
		if (start == end) end++;
		Range range = new Range(start, end);
		dataLayer.fireLayerEvent(new ColumnResizeEvent(dataLayer, range));

		return true;
	}

}
