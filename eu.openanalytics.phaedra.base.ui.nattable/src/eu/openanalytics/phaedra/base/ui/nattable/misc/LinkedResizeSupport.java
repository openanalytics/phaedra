package eu.openanalytics.phaedra.base.ui.nattable.misc;

import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.data.IColumnPropertyAccessor;
import org.eclipse.nebula.widgets.nattable.hideshow.event.HideRowPositionsEvent;
import org.eclipse.nebula.widgets.nattable.hideshow.event.ShowRowPositionsEvent;
import org.eclipse.nebula.widgets.nattable.layer.event.RowStructuralRefreshEvent;
import org.eclipse.nebula.widgets.nattable.resize.event.ColumnResizeEvent;
import org.eclipse.nebula.widgets.nattable.resize.event.RowResizeEvent;
import org.eclipse.swt.widgets.Display;

import eu.openanalytics.phaedra.base.ui.nattable.NatTableUtils;
import eu.openanalytics.phaedra.base.util.CollectionUtils;

/**
 * This class can be used to 'link' together several columns. Once linked, these columns will
 * resize together: if one of them is resized, the others will automatically be resized too.
 * More precisely:
 * <ol>
 * <li>The specified columns have the same width and height at all times</li>
 * <li>If the width of a column changes, its height automatically changes too, according to the aspect ratio</li>
 * <li>If the height of a column changes, its width automatically changes too, according to the aspect ratio</li>
 * </ol>
 */
public class LinkedResizeSupport {

	private SizeManager sm;

	public LinkedResizeSupport(float aspectRatio, IResizeCallback resizeCallback, ILinkedColumnAccessor<?> columnAccessor) {
		sm = new SizeManager();
		sm.startRow = 3; // Assumes 2 header rows and 1 filter row
		sm.aspectRatio = aspectRatio;
		sm.linkedColumnAccessor = columnAccessor;
		sm.callback = resizeCallback;
	}

	public void apply(NatTable table, boolean isGroupBy) {
		if (isGroupBy) sm.startRow += 2; // Add 2 rows for groupBy functionality
		sm.table = table;
		table.setData("sizeManager", sm);
		table.addLayerListener(event -> {
			if (event instanceof ColumnResizeEvent) {
				int colPosition = ((ColumnResizeEvent) event).getColumnPositionRanges().iterator().next().start;
				// Note: linkedColumns do not count the first (rowNr) column, skip them.
				if (colPosition < 1) return;
				int colIndex = sm.table.getColumnIndexByPosition(colPosition);
				int[] linkedColumns = sm.linkedColumnAccessor.getLinkedColumns();
				if (CollectionUtils.contains(linkedColumns, colIndex)) handleColumnResize(sm, colPosition);
			} else if (event instanceof RowResizeEvent) {
				int rowPosition = ((RowResizeEvent) event).getRowPositionRanges().iterator().next().start;
				if (rowPosition >= sm.startRow) handleRowResize(sm, rowPosition);
			} else if (isGroupBy) {
				// Support for expanding/collapsing rows in Group By supporting tables.
				if (event instanceof HideRowPositionsEvent || event instanceof ShowRowPositionsEvent
							|| event instanceof RowStructuralRefreshEvent) {

					resetSize(sm);
				}
			}
		});

		int[] linkedColumns = sm.linkedColumnAccessor.getLinkedColumns();
		if (linkedColumns.length > 0) {
			Display.getDefault().asyncExec(() -> handleColumnResize(sm, linkedColumns[0]-1));
		}
	}

	private static void handleColumnResize(SizeManager sm, int colPosition) {
		int newWidth = sm.table.getColumnWidthByPosition(colPosition);
		if (newWidth == 0 || sm.currentWidth == newWidth) return;
		sm.currentWidth = newWidth;
		sm.currentHeight = (int) (sm.currentWidth / sm.aspectRatio);
		resetSize(sm);
	}

	private static void handleRowResize(SizeManager sm, int rowPosition) {
		int newHeight = sm.table.getRowHeightByPosition(rowPosition);
		if (newHeight == 0 || sm.currentHeight == newHeight) return;
		sm.currentHeight = newHeight;
		sm.currentWidth = (int) (sm.currentHeight * sm.aspectRatio);
		resetSize(sm);
	}

	private static void resetSize(SizeManager sm) {
		NatTableUtils.resizeAllRows(sm.table, sm.currentHeight);
		NatTableUtils.resizeColumns(sm.table, sm.linkedColumnAccessor.getLinkedColumns(), sm.currentWidth);
		sm.callback.resized(sm.currentWidth, sm.currentHeight);
	}

	public static class SizeManager {
		public NatTable table;
		public int currentWidth;
		public int currentHeight;
		public float aspectRatio;
		public int startRow;
		public ILinkedColumnAccessor<?> linkedColumnAccessor;
		public IResizeCallback callback;
	}

	public static interface IResizeCallback {
		public void resized(int newWidth, int newHeight);
	}

	public static interface ILinkedColumnAccessor<T> extends IColumnPropertyAccessor<T> {
		public int[] getLinkedColumns();
	}
}
