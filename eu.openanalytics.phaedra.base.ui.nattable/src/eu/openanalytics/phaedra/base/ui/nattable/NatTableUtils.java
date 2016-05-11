package eu.openanalytics.phaedra.base.ui.nattable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.command.VisualRefreshCommand;
import org.eclipse.nebula.widgets.nattable.config.CellConfigAttributes;
import org.eclipse.nebula.widgets.nattable.config.IConfigRegistry;
import org.eclipse.nebula.widgets.nattable.data.IColumnPropertyAccessor;
import org.eclipse.nebula.widgets.nattable.data.IRowDataProvider;
import org.eclipse.nebula.widgets.nattable.data.convert.IDisplayConverter;
import org.eclipse.nebula.widgets.nattable.edit.EditConfigAttributes;
import org.eclipse.nebula.widgets.nattable.edit.editor.ComboBoxCellEditor;
import org.eclipse.nebula.widgets.nattable.extension.glazedlists.groupBy.GroupByConfigAttributes;
import org.eclipse.nebula.widgets.nattable.extension.glazedlists.groupBy.GroupByObject;
import org.eclipse.nebula.widgets.nattable.filterrow.FilterRowDataLayer;
import org.eclipse.nebula.widgets.nattable.filterrow.config.FilterRowConfigAttributes;
import org.eclipse.nebula.widgets.nattable.grid.layer.GridLayer;
import org.eclipse.nebula.widgets.nattable.hideshow.command.ColumnHideCommand;
import org.eclipse.nebula.widgets.nattable.hideshow.command.MultiColumnHideCommand;
import org.eclipse.nebula.widgets.nattable.layer.DataLayer;
import org.eclipse.nebula.widgets.nattable.layer.ILayer;
import org.eclipse.nebula.widgets.nattable.layer.IUniqueIndexLayer;
import org.eclipse.nebula.widgets.nattable.layer.cell.ILayerCell;
import org.eclipse.nebula.widgets.nattable.painter.cell.ICellPainter;
import org.eclipse.nebula.widgets.nattable.print.command.TurnViewportOffCommand;
import org.eclipse.nebula.widgets.nattable.print.command.TurnViewportOnCommand;
import org.eclipse.nebula.widgets.nattable.resize.command.ColumnResizeCommand;
import org.eclipse.nebula.widgets.nattable.resize.command.MultiColumnResizeCommand;
import org.eclipse.nebula.widgets.nattable.style.DisplayMode;
import org.eclipse.nebula.widgets.nattable.summaryrow.FixedSummaryRowLayer;
import org.eclipse.nebula.widgets.nattable.summaryrow.SummaryDisplayConverter;
import org.eclipse.nebula.widgets.nattable.summaryrow.SummaryRowConfigAttributes;
import org.eclipse.nebula.widgets.nattable.util.ArrayUtil;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;

import ca.odell.glazedlists.EventList;
import eu.openanalytics.phaedra.base.ui.nattable.command.FastMultiRowResizeCommand;
import eu.openanalytics.phaedra.base.ui.nattable.extension.glazedlists.groupBy.summary.StatsGroupBySummaryProvider;
import eu.openanalytics.phaedra.base.ui.nattable.layer.FullFeaturedColumnHeaderLayerStack;
import eu.openanalytics.phaedra.base.ui.nattable.summaryrow.StatsSummaryProvider;
import eu.openanalytics.phaedra.base.util.CollectionUtils;
import eu.openanalytics.phaedra.base.util.threading.JobUtils;

public class NatTableUtils {

	/**
	 * Resize all the rows to the default height (20) for given Table.
	 *
	 * @param table The NatTable, expects a GridLayer as child layer
	 */
	public static void resizeAllRows(NatTable table) {
		resizeAllRows(table, DataLayer.DEFAULT_ROW_HEIGHT);
	}

	/**
	 * Resize all the rows to the specified height for given Table.
	 *
	 * @param table The NatTable, expects a GridLayer as child layer
	 * @param rowHeight The new row height
	 */
	public static void resizeAllRows(NatTable table, int rowHeight) {
		table.doCommand(new TurnViewportOffCommand());
		ILayer bodyLayer = getGridLayer(table).getBodyLayer();
		table.doCommand(new FastMultiRowResizeCommand(bodyLayer, rowHeight));
		table.doCommand(new TurnViewportOnCommand());
	}

	/**
	 * Resize all the columns to the specified width for given Table.
	 *
	 * @param table The NatTable, expects a GridLayer as child layer
	 * @param colWidth The new column width
	 */
	public static void resizeAllColumns(NatTable table, int colWidth) {
		ILayer bodyLayer = getGridLayer(table).getBodyLayer();
		table.doCommand(new TurnViewportOffCommand());
		int[] columnPositions = CollectionUtils.fillIncrementingArray(bodyLayer.getColumnCount(), 0);
		table.doCommand(new MultiColumnResizeCommand(bodyLayer, columnPositions, colWidth));
		table.doCommand(new TurnViewportOnCommand());
	}

	/**
	 * Auto Resize all the columns using only the currently visible rows for given Table.
	 *
	 * @param table The NatTable, expects a GridLayer as child layer.
	 */
	public static void autoResizeAllColumns(NatTable table) {
		GridLayer gridLayer = getGridLayer(table);
		ILayer bodyLayer = gridLayer.getBodyLayer();
		int columnHeaderRows = gridLayer.getColumnHeaderLayer().getRowCount();
		int cornerColumns = gridLayer.getCornerLayer().getColumnCount();
		// Get visible rows before turning off viewport.
		int rowStart = bodyLayer.getRowIndexByPosition(0);
		int rowEnd = rowStart + bodyLayer.getRowCount();

		table.doCommand(new TurnViewportOffCommand());
		int[] columnPositions = CollectionUtils.fillIncrementingArray(bodyLayer.getColumnCount(), 0);
		int[] columnWidths = new int[columnPositions.length];

		// Get the max column width for visible rows.
		IConfigRegistry configRegistry = table.getConfigRegistry();
		GC gc = new GC(table);
		for (int col = 0; col < columnPositions.length; col++) {
			int dataWidth = getPreferredColumnWidth(bodyLayer, rowStart, rowEnd, columnPositions[col], configRegistry, gc);
			int columnWidth = getPreferredColumnWidth(gridLayer, 0, columnHeaderRows, columnPositions[col] + cornerColumns, configRegistry, gc);
			columnWidths[col] = Math.max(dataWidth, columnWidth);
		}
		gc.dispose();

		table.doCommand(new MultiColumnResizeCommand(bodyLayer, columnPositions, columnWidths));
		table.doCommand(new TurnViewportOnCommand());
	}

	public static void resizeColumn(NatTable table, int colIndex, int colWidth) {
		// Do not use ColumnResizeCommand to resize a single column.
		// The SelectionLayer converts it to a MultiColumnResizeCommand for all fully selected columns.
		resizeColumns(table, new int[] { colIndex }, colWidth);
	}

	public static void resizeColumns(NatTable table, int[] colIndexes, int colWidth) {
		ILayer bodyLayer = getGridLayer(table).getBodyLayer();
		table.doCommand(new TurnViewportOffCommand());
		int[] colPositions = convertColumnIndexToPosition(bodyLayer, colIndexes);
		table.doCommand(new MultiColumnResizeCommand(bodyLayer, colPositions, colWidth));
		table.doCommand(new TurnViewportOnCommand());
	}

	public static void resizeColumns(NatTable table, int[] colWidths) {
		ILayer bodyLayer = getGridLayer(table).getBodyLayer();
		table.doCommand(new TurnViewportOffCommand());
		for (int i = 0; i < colWidths.length; i++) {
			int colPosition = convertColumnIndexToPosition(bodyLayer, i);
			table.doCommand(new ColumnResizeCommand(bodyLayer, colPosition, colWidths[i]));
		}
		table.doCommand(new TurnViewportOnCommand());
	}

	public static <T> void resizeImageColumn(NatTable table, IRowDataProvider<T> rowDataProvider, EventList<T> eventList
			, Rectangle[] imageBounds, int padding) {

		resizeImageColumn(table, rowDataProvider, eventList, -1, imageBounds, padding);
	}

	public static <T> void resizeImageColumn(NatTable table, IRowDataProvider<T> rowDataProvider, EventList<T> eventList
			, int imageColumnPosition, Rectangle[] imageBounds, int padding) {

		if (table.isDisposed()) return;
		int rowCount = eventList.size();
		if (rowCount > imageBounds.length) return;
		AtomicInteger newWidth = new AtomicInteger(15);
		try {
			table.doCommand(new TurnViewportOffCommand());
			ILayer bodyLayer = getGridLayer(table).getBodyLayer();
			int[] rowPositions = new int[rowCount];
			int[] rowHeights = new int[rowCount];

			// Parallel is up to 3 times faster for big tables, not noticeably slower for small tables.
			IntStream.range(0, rowCount).parallel().forEach(i -> {
				rowPositions[i] = i;
				T object;
				try {
					object = rowDataProvider.getRowObject(i);
				} catch (IndexOutOfBoundsException e) {
					// When the data is being changed for the EventList, it is locked.
					// By the time getRowObject() can access it, the size can be reduced.
					rowHeights[i] = DataLayer.DEFAULT_ROW_HEIGHT;
					return;
				}
				if (object instanceof GroupByObject) {
					rowHeights[i] = DataLayer.DEFAULT_ROW_HEIGHT;
				} else if (object instanceof Integer) {
					Integer row = (Integer) object;
					Rectangle bounds = imageBounds[row];
					rowHeights[i] = Math.max(DataLayer.DEFAULT_ROW_HEIGHT, bounds.height) + padding;
					if (bounds.width > newWidth.get()) newWidth.set(bounds.width);
				} else {
					int indexOf = eventList.indexOf(object);
					Rectangle bounds = imageBounds[indexOf];
					rowHeights[i] = Math.max(DataLayer.DEFAULT_ROW_HEIGHT, bounds.height) + padding;
					if (bounds.width > newWidth.get()) newWidth.set(bounds.width);
				}
			});

			table.doCommand(new FastMultiRowResizeCommand(bodyLayer, ArrayUtil.asIntArray(rowPositions), ArrayUtil.asIntArray(rowHeights)));
			// Use a MultiColumnResizeCommand to prevent the SelectionLayer from turning the ColumnResizeCommand
			// into a MultiColumnResizeCommand for all columns.
			table.doCommand(new MultiColumnResizeCommand(bodyLayer, new int[] { imageColumnPosition }, newWidth.get() + padding));
			//table.doCommand(new ColumnResizeCommand(bodyLayer, imageColumnPosition, newWidth + padding));
			table.doCommand(new VisualRefreshCommand());
		} finally {
			table.doCommand(new TurnViewportOnCommand());
		}
	}

	public static void hideColumn(NatTable table, int colIndex) {
		GridLayer gridLayer = getGridLayer(table);
		int colPosition = convertColumnIndexToPosition(gridLayer, colIndex);
		if (colPosition >= 0) {
			table.doCommand(new TurnViewportOffCommand());
			table.doCommand(new ColumnHideCommand(gridLayer, colPosition));
			table.doCommand(new TurnViewportOnCommand());
		}
	}

	public static void hideColumns(NatTable table, int[] colIndexes) {
		table.doCommand(new TurnViewportOffCommand());
		int[] colPositions = convertColumnIndexToPosition(getGridLayer(table), colIndexes);
		table.doCommand(new MultiColumnHideCommand(table, colPositions));
		table.doCommand(new TurnViewportOnCommand());
	}

	@Deprecated
	public static int convertColumnIndexToPosition(NatTable table, int colIndex) {
		try {
			table.doCommand(new TurnViewportOffCommand());
			return convertColumnIndexToPosition(getGridLayer(table), colIndex);
		} finally {
			table.doCommand(new TurnViewportOnCommand());
		}
	}

	public static int getPreferredHeight(NatTable table, int columnPosition, int rowPosition) {
		IConfigRegistry configRegistry = table.getConfigRegistry();
		ILayer bodyLayer = ((GridLayer) table.getLayer().getUnderlyingLayerByPosition(0, 1)).getBodyLayer();
		ILayerCell cell = bodyLayer.getCellByPosition(columnPosition, rowPosition);
		ICellPainter cellPainter = table.getCellPainter(cell.getColumnPosition(), cell.getRowPosition(), cell, configRegistry);

		GC gc = null;
		try {
			gc = new GC(table);
			return cellPainter.getPreferredHeight(cell, gc, configRegistry);
		} finally {
			if (gc != null) gc.dispose();
		}
	}

	/**
	 * <p>Returns a list containing all the column names of given column accessor.</p>
	 *
	 * @param columnAccessor
	 * @return The list of column names.
	 */
	public static List<String> getColumnNames(IColumnPropertyAccessor<?> columnAccessor) {
		List<String> columnNames = new ArrayList<>();
		for (int i = 0; i < columnAccessor.getColumnCount(); i++) {
			String columnName = columnAccessor.getColumnProperty(i);
			columnNames.add(columnName);
		}
		return columnNames;
	}

	public static String getColumnName(NatTable table, int columnPosition) {
		GridLayer gridLayer = getGridLayer(table);
		ILayer columnHeaderLayer = gridLayer.getColumnHeaderLayer();

		int offset = gridLayer.getCornerLayer().getColumnCount();
		if (columnHeaderLayer instanceof FullFeaturedColumnHeaderLayerStack) {
			return columnHeaderLayer.getDataValueByPosition(columnPosition - offset, 1).toString();
		} else {
			return columnHeaderLayer.getDataValueByPosition(columnPosition - offset, 0).toString();
		}
	}

	public static void preload(NatTable table, IRowDataProvider<?> rowDataProvider) {
		ILayer bodyLayer = getGridLayer(table).getBodyLayer();
		int firstVisibleRow = bodyLayer.getRowIndexByPosition(0);
		int lastVisibleRow = firstVisibleRow + bodyLayer.getRowCount();
		int firstVisibleColumn = bodyLayer.getColumnIndexByPosition(0);
		int lastVisibleColumn = firstVisibleColumn + bodyLayer.getColumnCount();

		int rowsToPreload = lastVisibleRow - firstVisibleRow + 5;
		int columnsToPreload = lastVisibleColumn - firstVisibleColumn + 5;

		List<ILayerCell> cells = new ArrayList<>();
		table.doCommand(new TurnViewportOffCommand());
		for (int col = firstVisibleColumn - columnsToPreload; col < lastVisibleColumn + columnsToPreload; col++) {
			for (int row = firstVisibleRow - rowsToPreload; row < lastVisibleRow + rowsToPreload; row++) {
				if (col > firstVisibleColumn && col < lastVisibleColumn && row > firstVisibleRow && row < lastVisibleRow) continue;

				ILayerCell cell = bodyLayer.getCellByPosition(col, row);
				if (cell != null) cells.add(cell);
			}
		}
		table.doCommand(new TurnViewportOnCommand());

		JobUtils.runBackgroundJob(monitor -> {
			// Retrieve value so it can be cached.
			for (ILayerCell cell : cells) {
				if (monitor.isCanceled()) return;
				rowDataProvider.getDataValue(cell.getColumnIndex(), cell.getRowIndex());
			}
		}, rowDataProvider.toString(), null);
	}

	public static void applyAdvancedFilter(IConfigRegistry config, int columnIndex, IDisplayConverter converter, Comparator<?> comparator) {
		// This will be used for converting the entered filter.
		config.registerConfigAttribute(
				FilterRowConfigAttributes.FILTER_DISPLAY_CONVERTER
				, converter
				, DisplayMode.NORMAL
				, FilterRowDataLayer.FILTER_ROW_COLUMN_LABEL_PREFIX + columnIndex
		);
		// The comparator will be used to decide if a value is bigger, smaller or equal (1, -1, 0)
		config.registerConfigAttribute(
				FilterRowConfigAttributes.FILTER_COMPARATOR
				, comparator
				, DisplayMode.NORMAL
				, FilterRowDataLayer.FILTER_ROW_COLUMN_LABEL_PREFIX + columnIndex
		);
		// Now added by default in the PhaedraFilterRowConfiguration.
		//// Use regular expressions which support the following actions: <>, =, <(=) and >(=)
		//config.registerConfigAttribute(
		//		FilterRowConfigAttributes.TEXT_MATCHING_MODE
		//		, TextMatchingMode.REGULAR_EXPRESSION
		//		, DisplayMode.NORMAL
		//		, FilterRowDataLayer.FILTER_ROW_COLUMN_LABEL_PREFIX + columnIndex
		//);
	}

	/**
	 * Adds a Filter Combo for the given column.
	 *
	 * @param config The ConfigRegistry of used NatTable
	 * @param columnIndex The column index for which you want advanced filtering
	 * @param list The list of available filter items
	 * @param comboConverter The IDisplayConverter for the combo list
	 * @param filterCellConverter The IDisplayConverter for the filter cell
	 * @param filterConverter The IDisplayConverter used for the actual filtering
	 */
	public static void applyAdvancedComboFilter(IConfigRegistry config, int columnIndex, List<?> list
			, IDisplayConverter comboConverter, IDisplayConverter filterCellConverter, IDisplayConverter filterConverter) {

		// Create comboBoxCellEditor filter for given column.
		ComboBoxCellEditor comboBoxCellEditor = new ComboBoxCellEditor(list, 6);
		comboBoxCellEditor.setMultiselect(true);
		comboBoxCellEditor.setFreeEdit(true);
		comboBoxCellEditor.setMultiselectTextBracket("", "");

		// Tell NatTable to use the comboBoxCellEditor for given column.
		config.registerConfigAttribute(
				EditConfigAttributes.CELL_EDITOR
				, comboBoxCellEditor
				, DisplayMode.NORMAL
				, FilterRowDataLayer.FILTER_ROW_COLUMN_LABEL_PREFIX + columnIndex
		);

		// Display Converter for the combo box.
		config.registerConfigAttribute(
				CellConfigAttributes.DISPLAY_CONVERTER
				, comboConverter
				, DisplayMode.EDIT
				, FilterRowDataLayer.FILTER_ROW_COLUMN_LABEL_PREFIX + columnIndex
		);
		// Display Converter for the filter cell.
		config.registerConfigAttribute(
				CellConfigAttributes.DISPLAY_CONVERTER
				, filterCellConverter
				, DisplayMode.NORMAL
				, FilterRowDataLayer.FILTER_ROW_COLUMN_LABEL_PREFIX + columnIndex
		);
		// Display Converter for the actual filtering.
		config.registerConfigAttribute(
				FilterRowConfigAttributes.FILTER_DISPLAY_CONVERTER
				, filterConverter
				, DisplayMode.NORMAL
				, FilterRowDataLayer.FILTER_ROW_COLUMN_LABEL_PREFIX + columnIndex
		);
	}

	public static void applySummaryProvider(NatTable table, IColumnPropertyAccessor<?> columnAccessor, int columnIndex,
			String columnName, IDisplayConverter displayConverter) {

		IConfigRegistry configRegistry = table.getConfigRegistry();

		// Add Group By summary support.
		configRegistry.registerConfigAttribute(
				GroupByConfigAttributes.GROUP_BY_SUMMARY_PROVIDER
				, new StatsGroupBySummaryProvider<>(columnAccessor)
				, DisplayMode.NORMAL, columnName
		);
		// Add Summary Row support.
		configRegistry.registerConfigAttribute(
				SummaryRowConfigAttributes.SUMMARY_PROVIDER
				, new StatsSummaryProvider(table)
				, DisplayMode.NORMAL
				, FixedSummaryRowLayer.DEFAULT_SUMMARY_COLUMN_CONFIG_LABEL_PREFIX + columnIndex
		);
		configRegistry.registerConfigAttribute(
                CellConfigAttributes.DISPLAY_CONVERTER
                , new SummaryDisplayConverter(displayConverter)
                , DisplayMode.NORMAL
                , FixedSummaryRowLayer.DEFAULT_SUMMARY_COLUMN_CONFIG_LABEL_PREFIX + columnIndex
        );
	}

	private static int[] convertColumnIndexToPosition(ILayer layer, int[] colIndexes) {
		int[] colPositions = new int[colIndexes.length];
		for (int i = 0; i < colIndexes.length; i++) {
			colPositions[i] = convertColumnIndexToPosition(layer, colIndexes[i]);
		}
		return colPositions;
	}

	private static int convertColumnIndexToPosition(ILayer layer, int colIndex) {
		if (layer instanceof IUniqueIndexLayer) return ((IUniqueIndexLayer) layer).getColumnPositionByIndex(colIndex);
		for (int colPosition = 0; colPosition < layer.getPreferredColumnCount(); colPosition++) {
			int columnIndexByPosition = layer.getColumnIndexByPosition(colPosition);
			if (colIndex == columnIndexByPosition) return colPosition;
		}
		return -1;
	}

	private static int getPreferredColumnWidth(ILayer layer, int rowStart, int rowEnd, int columnPosition, IConfigRegistry configRegistry, GC gc) {
		ICellPainter painter;
		int maxWidth = 0;
		ILayerCell cell;

		for (int rowPosition = rowStart; rowPosition < rowEnd; rowPosition++) {
			cell = layer.getCellByPosition(columnPosition, rowPosition);
			if (cell != null) {
				boolean atEndOfCellSpan = cell.getOriginColumnPosition() + cell.getColumnSpan() - 1 == columnPosition;
				if (atEndOfCellSpan) {
					painter = layer.getCellPainter(cell.getColumnPosition(), cell.getRowPosition(), cell, configRegistry);
					if (painter != null) {
						int preferredWidth = painter.getPreferredWidth(cell, gc, configRegistry);

						// Adjust width
						Rectangle bounds = cell.getBounds();
						bounds.width = preferredWidth;
						Rectangle adjustedCellBounds = cell.getLayer().getLayerPainter().adjustCellBounds(columnPosition, rowPosition, bounds);
						preferredWidth += preferredWidth - adjustedCellBounds.width;

						if (cell.getColumnSpan() > 1) {
							int columnStartX = layer.getStartXOfColumnPosition(columnPosition);
							int cellStartX = layer.getStartXOfColumnPosition(cell.getOriginColumnPosition());
							preferredWidth = Math.max(0, preferredWidth - (columnStartX - cellStartX));
						}

						maxWidth = (preferredWidth > maxWidth) ? preferredWidth : maxWidth;
					}
				}
			}
		}

		return maxWidth;
	}

	private static GridLayer getGridLayer(NatTable table) {
		ILayer layer = table.getLayer();
		if (layer instanceof GridLayer) {
			return (GridLayer) layer;
		} else {
			return (GridLayer) layer.getUnderlyingLayerByPosition(0, 1);
		}
	}

}