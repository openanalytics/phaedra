package eu.openanalytics.phaedra.base.ui.nattable.command;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.nebula.widgets.nattable.command.AbstractLayerCommandHandler;
import org.eclipse.nebula.widgets.nattable.coordinate.Range;
import org.eclipse.nebula.widgets.nattable.extension.glazedlists.groupBy.GroupByDataLayer;
import org.eclipse.nebula.widgets.nattable.layer.DataLayer;
import org.eclipse.nebula.widgets.nattable.layer.SizeConfig;
import org.eclipse.nebula.widgets.nattable.resize.command.MultiRowResizeCommandHandler;
import org.eclipse.nebula.widgets.nattable.resize.event.RowResizeEvent;

import eu.openanalytics.phaedra.base.ui.nattable.Activator;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;

/**
 * <p>Based on {@link MultiRowResizeCommandHandler}</p>
 *
 * <p>Sends only one event with a range that spans all the changed rows, this might include rows from which the size was not changed.</p>
 */
public class FastMultiRowResizeCommandHandler extends AbstractLayerCommandHandler<FastMultiRowResizeCommand> {

	private final DataLayer dataLayer;

	public FastMultiRowResizeCommandHandler(DataLayer dataLayer) {
		this.dataLayer = dataLayer;
	}

	@Override
	public Class<FastMultiRowResizeCommand> getCommandClass() {
		return FastMultiRowResizeCommand.class;
	}

	@Override
	protected boolean doCommand(FastMultiRowResizeCommand command) {
		if (command.isResizeAllRows()) {
			if (dataLayer instanceof GroupByDataLayer) {
				for (int row = 0; row < dataLayer.getRowCount(); row++) {
					if (dataLayer.getConfigLabelsByPosition(0, row).hasLabel(GroupByDataLayer.GROUP_BY_OBJECT)) {
						dataLayer.setRowHeightByPosition(row, DataLayer.DEFAULT_ROW_HEIGHT, false);
					} else {
						dataLayer.setRowHeightByPosition(row, command.getCommonRowHeight(), false);
					}
				}

				// Fire event to properly refresh table.
				Range range = new Range(0, dataLayer.getRowCount());
				dataLayer.fireLayerEvent(new RowResizeEvent(dataLayer, range));

				return true;
			}
			// A common row height was configured.
			// Use reflection to clear the size map and set the default size to improve paint performance for large tables.
			try {
				// Get the SizeConfig object from the DataLayer.
				SizeConfig rowHeightConfig = (SizeConfig) getFieldValue(dataLayer, DataLayer.class, "rowHeightConfig");
				// Set the (new) default size. This will also clear the SizeConfig cache.
				rowHeightConfig.setDefaultSize(command.getCommonRowHeight());
				// Get the sizeMap from the SizeConfig which contains all the custom sizes for each row.
				Map<?, ?> sizeMap = (Map<?, ?>) getFieldValue(rowHeightConfig, SizeConfig.class, "sizeMap");
				// Clear it so it will always use the default size.
				sizeMap.clear();

				// Fire event to properly refresh table.
				Range range = new Range(0, dataLayer.getRowCount());
				dataLayer.fireLayerEvent(new RowResizeEvent(dataLayer, range));
			} catch (ReflectiveOperationException e) {
				EclipseLog.error(e.getMessage(), e, Activator.getDefault());
				return false;
			}
		} else {
			List<Integer> rowPositions = Collections.synchronizedList(new ArrayList<Integer>());

			// Individual sizes have been configured for each given row.
			for (int rowPosition : command.getRowPositions()) {
				rowPositions.add(rowPosition);
				dataLayer.setRowHeightByPosition(rowPosition, command.getRowHeight(rowPosition), false);
			}

			// Only send one event with a range that contains all rows (even some that were not changed).
			int start = Integer.MAX_VALUE;
			int end = 0;
			for (Integer rowPosition : rowPositions) {
				start = Math.min(start, rowPosition);
				end = Math.max(end, rowPosition);
			}
			if (start == end) end++;

			// Fire event to properly refresh table.
			Range range = new Range(start, end);
			dataLayer.fireLayerEvent(new RowResizeEvent(dataLayer, range));
		}

		return true;
	}

	private <T> Object getFieldValue(T object, Class<T> clazz, String fieldName) throws ReflectiveOperationException {
		Field field = clazz.getDeclaredField(fieldName);
		boolean isAccessible = field.isAccessible();
		try {
			field.setAccessible(true);
			return field.get(object);
		} finally {
			field.setAccessible(isAccessible);
		}
	}

}
