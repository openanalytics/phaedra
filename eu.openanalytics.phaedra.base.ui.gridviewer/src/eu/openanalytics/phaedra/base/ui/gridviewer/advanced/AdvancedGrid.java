package eu.openanalytics.phaedra.base.ui.gridviewer.advanced;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;

import eu.openanalytics.phaedra.base.ui.gridviewer.widget.Grid;
import eu.openanalytics.phaedra.base.ui.gridviewer.widget.GridCell;
import eu.openanalytics.phaedra.base.ui.gridviewer.widget.GridHeaderCell;
import eu.openanalytics.phaedra.base.ui.gridviewer.widget.GridHeaderCell.Type;
import eu.openanalytics.phaedra.base.ui.util.highlighter.State;

public class AdvancedGrid extends Grid {

	private static final int DEFAULT_CELL_SIZE = 250;
	private static final int DEFAULT_COLUMN_ROW_COUNT = 5;

	private int cellWidth;
	private int cellHeight;

	private int preferredColumns;
	private int preferredRows;

	private boolean keepSquare;

	private int hScrollOffset;
	private int vScrollOffset;
	private int maxWidth;
	private int maxHeight;
	private int clientAreaWidth;
	private int clientAreaHeight;
	private int lastSize;

	private GridFillLayout gridFillLayout;

	public enum GridFillLayout {
		SPECIFY_ROW("Specify # of Rows")
		, SPECIFY_COLUMN("Specify # of Columns")
		, VERTICAL_FILL("Vertical Fill")
		, HORIZONTAL_FILL("Horizontal Fill")
		;

		private String name;

		private GridFillLayout(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}
	}

	public AdvancedGrid(Composite parent, int style, int rows, int columns) {
		super(parent, style | SWT.H_SCROLL | SWT.V_SCROLL, rows, columns);

		this.keepSquare = true;
		this.cellWidth = DEFAULT_CELL_SIZE;
		this.cellHeight = DEFAULT_CELL_SIZE;
		this.preferredColumns = DEFAULT_COLUMN_ROW_COUNT;
		this.preferredRows = DEFAULT_COLUMN_ROW_COUNT;

		getHorizontalBar().addListener(SWT.Selection, e -> scrollH());
		getVerticalBar().addListener(SWT.Selection, e -> scrollV());

		addListener(SWT.Resize, e -> resize());
	}

	public GridFillLayout getGridFillLayout() {
		return gridFillLayout;
	}

	public void setGridFillLayout(GridFillLayout gridFillLayout) {
		this.gridFillLayout = gridFillLayout;
	}

	public int getPreferredColumns() {
		return preferredColumns;
	}

	public void setPreferredColumns(int preferredColumns) {
		this.preferredColumns = preferredColumns;
	}

	public int getPreferredRows() {
		return preferredRows;
	}

	public void setPreferredRows(int preferredRows) {
		this.preferredRows = preferredRows;
	}

	public boolean isKeepSquare() {
		return keepSquare;
	}

	public void setKeepSquare(boolean keepSquare) {
		this.keepSquare = keepSquare;
	}

	public boolean isFillColumnWise() {
		if (gridFillLayout != null) {
			switch (gridFillLayout) {
			case SPECIFY_ROW:
			case VERTICAL_FILL:
				return false;
			default:
				break;
			}
		}
		return true;
	};

	public void resetGrid(int size) {
		this.lastSize = size;
		int columns = 0;
		int rows = 0;

		if (size > 0) {
			switch (gridFillLayout) {
			case SPECIFY_ROW:
				rows = preferredRows;
				if (rows > size) rows = size;
				columns = (size + rows - 1) / rows;
				cellHeight = (clientAreaHeight - headerSize - (rows * vSpacing)) / rows;
				break;
			case SPECIFY_COLUMN:
				columns = preferredColumns;
				if (columns > size) columns = size;
				rows = (size + columns - 1) / columns;
				cellWidth = (clientAreaWidth - headerSize - (columns * hSpacing)) / columns;
				break;
			case VERTICAL_FILL:
				rows = (clientAreaHeight - headerSize) / (cellHeight + vSpacing);
				if (rows > size) rows = size;
				else if (rows < 1) rows = 1;
				columns = (size + rows - 1) / rows;
				break;
			case HORIZONTAL_FILL:
				columns = (clientAreaWidth - headerSize) / (cellWidth + hSpacing);
				if (columns > size) columns = size;
				else if (columns < 1) columns = 1;
				rows = (size + columns - 1) / columns;
				break;
			default:
				double sqrt = Math.sqrt(size);
				columns = (int) Math.ceil(sqrt);
				rows = (int) Math.floor(sqrt);
				break;
			}
		}

		resetGrid(rows, columns);
	}

	@Override
	public void resetGrid(int rows, int columns) {
		if (getRows() == rows && getColumns() == columns) return;
		super.resetGrid(rows, columns);
		resize();
	}

	@Override
	protected void paint(GC gc) {
		// Get the start and end for cells so only visible cells are drawn.
		int rowStart = vScrollOffset / (cellHeight + hSpacing);
		int colStart = hScrollOffset / (cellWidth + vSpacing);
		int tempCellHeight = Math.max(1, cellHeight);
		int tempCellWidth = Math.max(1, cellWidth);
		int rowEnd = Math.min(getRows(), (clientAreaHeight + tempCellHeight) / tempCellHeight + rowStart + 1);
		int colEnd = Math.min(getColumns(), (clientAreaWidth + tempCellWidth) / tempCellWidth + colStart + 1);

		getCellRenderer().prerender(this);

		// Paint the body cells
		if (isFillColumnWise()) {
			for (int row=rowStart; row<rowEnd; row++) {
				for (int col=colStart; col<colEnd; col++) {
					GridCell cell = bodyCells[row][col];
					Rectangle bounds = calculateBounds(cell);
					cell.paint(gc, bounds.x, bounds.y, bounds.width, bounds.height);
				}
			}
		} else {
			for (int col=colStart; col<colEnd; col++) {
				for (int row=rowStart; row<rowEnd; row++) {
					GridCell cell = bodyCells[row][col];
					Rectangle bounds = calculateBounds(cell);
					cell.paint(gc, bounds.x, bounds.y, bounds.width, bounds.height);
				}
			}
		}

		// Paint the row headers
		for (int row=rowStart; row<rowEnd; row++) {
			GridCell header = rowHeaderCells[row];
			Rectangle bounds = calculateBounds(header);
			header.paint(gc, bounds.x, bounds.y, bounds.width, bounds.height);
		}

		// Paint the column headers
		for (int col=colStart; col<colEnd; col++) {
			GridCell header = columnHeaderCells[col];
			Rectangle bounds = calculateBounds(header);
			header.paint(gc, bounds.x, bounds.y, bounds.width, bounds.height);
		}

		paintDrag(gc);
	}

	@Override
	protected void paintHighlightState(State state) {
		if (getSelectedCells().isEmpty()) return;
		Rectangle area = getClientArea();
		GC gc = new GC(AdvancedGrid.this);
		try {
			getSelectedCells().forEach(c -> {
				c.getHighlight().setState(state);
				if (isDisposed()) return;
				Rectangle bounds = calculateBounds(c);
				if (bounds.x < -cellWidth || bounds.x > area.width || bounds.y < -cellHeight || bounds.y > area.height) {
					// Do not redraw cell highlighting for cells that are not visible.
					return;
				}
				c.getHighlight().paint(gc, bounds.x, bounds.y, bounds.width, bounds.height);
			});
		} finally {
			gc.dispose();
		}
	}

	@Override
	protected GridCell getCellAt(int x, int y) {
		x -= headerSize;
		y -= headerSize;

		x += hScrollOffset;
		y += vScrollOffset;
		int row = (y/(cellHeight+vSpacing));
		int col = (x/(cellWidth+hSpacing));

		if(row < 0) row = 0;
		if(col < 0) col = 0;
		if(row >= rowHeaderCells.length) row = rowHeaderCells.length-1;
		if(col >= columnHeaderCells.length) col = columnHeaderCells.length-1;

		if (x < 0) return rowHeaderCells[row];
		if (y < 0) return columnHeaderCells[col];
		if (x >= 0 && y >= 0 && row < bodyCells.length && col < bodyCells[row].length)
			return bodyCells[row][col];
		return null;
	}

	@Override
	public Rectangle calculateBounds(GridCell cell) {
		int row = cell.getRow();
		int col = cell.getColumn();
		int cellWidth = this.cellWidth;
		int cellHeight = this.cellHeight;
		int x = headerSize+(col*(cellWidth+hSpacing)) - hScrollOffset;
		int y = headerSize+(row*(cellHeight+vSpacing)) - vScrollOffset;

		if (cell instanceof GridHeaderCell) {
			GridHeaderCell c = (GridHeaderCell)cell;
			if (c.getType() == Type.Horizontal) {
				y = 0;
				cellHeight = headerSize;
			} else {
				x = 0;
				cellWidth = headerSize;
			}
		}
		return new Rectangle(x, y, cellWidth, cellHeight);
	}

	@Override
	public Point getCurrentCellSize() {
		return new Point(cellWidth, cellHeight);
	}

	public void setCurrentCellSize(int x, int y) {
		this.cellWidth = x;
		this.cellHeight = y;
		resize();
		redraw();
	}

	private void scrollH() {
		hScrollOffset = getHorizontalBar().getSelection();
		redraw();
	}
	private void scrollV() {
		vScrollOffset = getVerticalBar().getSelection();
		redraw();
	}

	private void updateHScroll() {
		if (maxWidth > clientAreaWidth) {
			getHorizontalBar().setEnabled(true);
			getHorizontalBar().setVisible(true);
			hScrollOffset = Math.min(hScrollOffset, maxWidth - clientAreaWidth);
			getHorizontalBar().setValues(hScrollOffset, 0, maxWidth, clientAreaWidth, 50, clientAreaWidth);
		} else {
			hScrollOffset = 0;
			getHorizontalBar().setEnabled(false);
			getHorizontalBar().setVisible(false);
		}
	}

	private void updateVScroll() {
		if (maxHeight > clientAreaHeight) {
			getVerticalBar().setEnabled(true);
			getVerticalBar().setVisible(true);
			vScrollOffset = Math.min(vScrollOffset, maxHeight - clientAreaHeight);
			getVerticalBar().setValues(vScrollOffset, 0, maxHeight, clientAreaHeight, 50, clientAreaHeight);
		} else {
			vScrollOffset = 0;
			getVerticalBar().setEnabled(false);
			getVerticalBar().setVisible(false);
		}
	}

	private void resize() {
		Rectangle clientArea = getClientArea();
		clientAreaWidth = clientArea.width;
		clientAreaHeight = clientArea.height;

		// If a gridFillLayout is specified we might need some extra calculations on a resize.
		if (gridFillLayout != null) {
			switch (gridFillLayout) {
			case SPECIFY_ROW:
				if (getRows() > 0) {
					cellHeight = (clientAreaHeight - headerSize - (getRows() * vSpacing)) / getRows();
					if (keepSquare) cellWidth = cellHeight;
				}
				break;
			case SPECIFY_COLUMN:
				if (getColumns() > 0) {
					cellWidth = (clientAreaWidth - headerSize - (getColumns() * hSpacing)) / getColumns();
					if (keepSquare) cellHeight = cellWidth;
				}
				break;
			case VERTICAL_FILL:
			case HORIZONTAL_FILL:
				Object[] datas = new Object[getRows() * getColumns()];
				int i = 0;
				if (isFillColumnWise()) {
					for (int row=0; row < getRows(); row++) {
						for (int col=0; col < getColumns(); col++) {
							datas[i++] = getCell(row, col).getData();
						}
					}
				} else {
					for (int col=0; col < getColumns(); col++) {
						for (int row=0; row < getRows(); row++) {
							datas[i++] = getCell(row, col).getData();
						}
					}
				}
				resetGrid(lastSize);
				i = 0;
				if (isFillColumnWise()) {
					for (int row=0; row < getRows(); row++) {
						for (int col=0; col < getColumns(); col++) {
							if (i < datas.length) getCell(row, col).setData(datas[i++]);
							else break;
						}
					}
				} else {
					for (int col=0; col < getColumns(); col++) {
						for (int row=0; row < getRows(); row++) {
							if (i < datas.length) getCell(row, col).setData(datas[i++]);
							else break;
						}
					}
				}
				break;
			default:
				break;
			}
		}

		maxWidth = headerSize + (getColumns() * (cellWidth + hSpacing));
		maxHeight = headerSize + (getRows() * (cellHeight + hSpacing));
		updateHScroll();
		updateVScroll();
	}

}
