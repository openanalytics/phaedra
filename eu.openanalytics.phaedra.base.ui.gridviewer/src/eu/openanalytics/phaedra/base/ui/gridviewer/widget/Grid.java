package eu.openanalytics.phaedra.base.ui.gridviewer.widget;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.jface.window.DefaultToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

import eu.openanalytics.phaedra.base.ui.gridviewer.Activator;
import eu.openanalytics.phaedra.base.ui.gridviewer.preferences.Prefs;
import eu.openanalytics.phaedra.base.ui.gridviewer.widget.GridHeaderCell.Type;
import eu.openanalytics.phaedra.base.ui.gridviewer.widget.render.IGridCellRenderer;
import eu.openanalytics.phaedra.base.ui.util.highlighter.HighlightListener;
import eu.openanalytics.phaedra.base.ui.util.highlighter.HighlightTimer;
import eu.openanalytics.phaedra.base.ui.util.highlighter.State;
import eu.openanalytics.phaedra.base.util.misc.SWTUtils;

public class Grid extends Canvas implements MouseListener, MouseMoveListener, KeyListener {

	private final static int DEFAULT_HEADER_SIZE = 15;

	protected GridCell[] columnHeaderCells;
	protected GridCell[] rowHeaderCells;
	protected GridCell[][] bodyCells;

	protected int headerSize = DEFAULT_HEADER_SIZE;
	protected int hSpacing = 2;
	protected int vSpacing = 2;

	private int rows;
	private int columns;

	private IGridCellRenderer cellRenderer;
	private PaintListener paintListener;

	private boolean tooltipEnabled;
	private boolean selectionEnabled;
	private boolean dragging;
	private Rectangle dragArea;

	private HighlightListener highlightListener;

	private Set<GridCell> selectedCells;
	private Color selectionBoxColor;
	private IGridSelectionListener selectionListener;

	public Grid(Composite parent, int style, int rows, int columns) {
		super(parent, style | SWT.DOUBLE_BUFFERED);

		resetGrid(rows, columns);

		tooltipEnabled = Activator.getDefault().getPreferenceStore().getBoolean(Prefs.GRID_TOOLTIPS);
		selectionEnabled = true;
		selectedCells = new HashSet<>();
		selectionBoxColor = new Color(parent.getDisplay(),100,100,100);

		paintListener = e -> paint(e.gc);
		addPaintListener(paintListener);

		addListener(SWT.Dispose, e -> {
			HighlightTimer.getInstance().removeListener(highlightListener);
			removePaintListener(paintListener);
			selectionBoxColor.dispose();
			if (cellRenderer != null) cellRenderer.dispose();
		});

		if (tooltipEnabled) {
			addListener(SWT.MouseHover, e -> {
				GridCell cell = getCellAt(e.x, e.y);
				if (cell != null) {
					String ttText = cellRenderer.getTooltipContribution(cell);
					if (ttText != null && !ttText.isEmpty()) {
						DefaultToolTip tt = new DefaultToolTip(Grid.this, DefaultToolTip.NO_RECREATE, false);
						tt.setShift(new Point(0, 20));
						tt.setText(ttText);
					}
				}
			});
		}

		highlightListener = state -> paintHighlightState(state);
		HighlightTimer.getInstance().addListener(highlightListener);

		addMouseListener(this);
		addMouseMoveListener(this);
		addKeyListener(this);
	}

	public void setShowHeaders(boolean showHeaders) {
		if (showHeaders) {
			if (headerSize > 0) return; // Already showing headers
			headerSize = DEFAULT_HEADER_SIZE;
		} else {
			if (headerSize == 0) return; // Already hiding headers
			headerSize = 0;
		}
		redraw();
	}

	public void hookSelectionListener(IGridSelectionListener listener) {
		this.selectionListener = listener;
	}

	public void setTooltipEnabled(boolean tooltipEnabled) {
		this.tooltipEnabled = tooltipEnabled;
	}

	public void setSelectionEnabled(boolean selectionEnabled) {
		this.selectionEnabled = selectionEnabled;
	}

	public Set<GridCell> getSelectedCells() {
		return selectedCells;
	}

	public void setSelectedCells(Set<GridCell> selectedCells) {
		this.selectedCells.forEach(c -> c.getHighlight().setState(State.Off));
		if (selectedCells != null && !selectedCells.isEmpty()) {
			this.selectedCells = selectedCells;
			this.selectedCells.forEach(c -> c.getHighlight().setState(State.On1));
		} else {
			this.selectedCells.clear();
		}
		redraw();
	}

	public int getRows() {
		return rows;
	}

	public int getColumns() {
		return columns;
	}

	public GridCell getCell(int row, int column) {
		return bodyCells[row][column];
	}

	public void resetGrid(int rows, int columns) {
		this.rows = rows;
		this.columns = columns;

		columnHeaderCells = new GridCell[columns];
		for (int col=0; col<columns; col++) {
			columnHeaderCells[col] = new GridHeaderCell(this, col, col, Type.Horizontal);
		}
		rowHeaderCells = new GridCell[rows];
		for (int row=0; row<rows; row++) {
			rowHeaderCells[row] = new GridHeaderCell(this, row, row, Type.Vertical);
		}
		bodyCells = new GridCell[rows][columns];
		for (int row=0; row<rows; row++) {
			for (int col=0; col<columns; col++) {
				bodyCells[row][col] = new GridCell(this, row, col);
			}
		}
	}

	public void setCellRenderer(IGridCellRenderer cellRenderer) {
		this.cellRenderer = cellRenderer;
	}

	protected IGridCellRenderer getCellRenderer() {
		return cellRenderer;
	}

	/*
	 * *********************************
	 * Key, mouse and selection handling
	 * *********************************
	 */

	public boolean isDragging() {
		return dragging;
	}

	@Override
	public void keyPressed(KeyEvent e) {
		if (!selectionEnabled) return;
	}

	@Override
	public void keyReleased(KeyEvent e) {
		if (!selectionEnabled) return;

		if (!selectedCells.isEmpty()) {
			GridCell cell = selectedCells.iterator().next();
			int row = cell.getRow();
			int col = cell.getColumn();

			boolean moved = (
					e.keyCode == SWT.ARROW_LEFT || e.keyCode == SWT.ARROW_RIGHT
					|| e.keyCode == SWT.ARROW_UP || e.keyCode == SWT.ARROW_DOWN);

			if (e.keyCode == SWT.ARROW_LEFT && col > 0) col--;
			else if (e.keyCode == SWT.ARROW_RIGHT && col < columns-1) col++;
			else if (e.keyCode == SWT.ARROW_UP && row > 0) row--;
			else if (e.keyCode == SWT.ARROW_DOWN && row < rows-1) row++;

			if (moved) {
				clearSelection();
				addToSelection(bodyCells[row][col]);
				handleSelection();
			}
		}
	}

	@Override
	public void mouseMove(MouseEvent e) {
		if (!selectionEnabled) return;

		if (dragging) {
			dragArea.width = e.x - dragArea.x;
			dragArea.height = e.y - dragArea.y;
			redraw();
		}
	}

	@Override
	public void mouseDoubleClick(MouseEvent e) {
		// Do nothing.
	}

	@Override
	public void mouseDown(MouseEvent e) {
		if (!selectionEnabled || e.button != 1) return;
		dragging = true;
		dragArea = new Rectangle(e.x,e.y,0,0);
	}

	@Override
	public void mouseUp(MouseEvent e) {
		if (!selectionEnabled || e.button != 1) return;

		dragging = false;
		if (dragArea == null) return;

		if (e.button == 3 && selectedCells.size() > 1) {
			return;
		}

		// The field variable can return wrong results here.
		boolean ctrlPressed = (e.stateMask & SWT.CTRL) > 0;
		if (!ctrlPressed) clearSelection();

		GridCell cell = getCellAt(e.x,e.y);

		if (cell instanceof GridHeaderCell) {
			// Header click
			GridHeaderCell header = (GridHeaderCell)cell;
			if (header.getType() == Type.Horizontal) {
				int col = header.getColumn();
				for (int row=0; row<rows; row++) {
					addToSelection(bodyCells[row][col]);
				}
			} else if (header.getType() == Type.Vertical) {
				int row = header.getRow();
				for (int col=0; col<columns; col++) {
					addToSelection(bodyCells[row][col]);
				}
			}
		} else {
			Point p = new Point(e.x,e.y);
			boolean areaSelect = (SWTUtils.getDistance(new Point(dragArea.x, dragArea.y), p) > 4);
			if (areaSelect) {
				SWTUtils.normalize(dragArea);
				handleAreaSelection();
			} else {
				if (ctrlPressed && selectedCells.contains(cell)) {
					cell.getHighlight().setState(State.Off);
					selectedCells.remove(cell);
				} else {
					if (cell != null) addToSelection(cell);
				}
			}
		}

		handleSelection();
		dragArea = null;
	}

	private void handleAreaSelection() {
		for (int row=0; row<rows; row++) {
			for (int col=0; col<columns; col++) {
				GridCell cell = bodyCells[row][col];
				Rectangle cellRectangle = calculateBounds(cell);
				if (cellRectangle.intersects(dragArea)) {
					addToSelection(cell);
				}
			}
		}
	}

	private void addToSelection(GridCell cell) {
		if (cell.getData() != null) {
			selectedCells.add(cell);
		}
	}

	private void handleSelection() {
		this.selectedCells.forEach(c -> c.getHighlight().setState(State.On1));
		redraw();
		if (selectionListener != null) {
			GridCell[] selection = selectedCells.toArray(new GridCell[selectedCells.size()]);
			selectionListener.selectionChanged(selection);
		}
	}

	private void clearSelection() {
		// Set the State to Off for the selection that is being cleared.
		this.selectedCells.forEach(c -> c.getHighlight().setState(State.Off));
		this.selectedCells.clear();
		// Redraw so no Selection is visible.
		redraw();
	}

	/*
	 * ***********************************
	 * Grid rendering and cell positioning
	 * ***********************************
	 */

	protected void paint(GC gc) {
		// Paint the row headers
		for (int row=0; row<rows; row++) {
			GridCell header = rowHeaderCells[row];
			Rectangle bounds = calculateBounds(header);
			header.paint(gc, bounds.x,bounds.y,bounds.width,bounds.height);
		}

		// Paint the column headers
		for (int col=0; col<columns; col++) {
			GridCell header = columnHeaderCells[col];
			Rectangle bounds = calculateBounds(header);
			header.paint(gc, bounds.x,bounds.y,bounds.width,bounds.height);
		}

		cellRenderer.prerender(this);

		// Paint the body cells
		for (int row=0; row<rows; row++) {
			for (int col=0; col<columns; col++) {
				GridCell cell = bodyCells[row][col];
				Rectangle bounds = calculateBounds(cell);
				cell.paint(gc, bounds.x,bounds.y,bounds.width,bounds.height);
			}
		}

		paintDrag(gc);
	}

	protected void paintDrag(GC gc) {
		if (dragging) {
			gc.setLineWidth(1);
			gc.setForeground(selectionBoxColor);
			gc.drawRectangle(dragArea);
		}
	}

	protected void paintHighlightState(State state) {
		if (selectedCells.isEmpty()) return;
		GC gc = new GC(Grid.this);
		try {
			selectedCells.forEach(c -> {
				c.getHighlight().setState(state);
				if (isDisposed()) return;
				Rectangle bounds = calculateBounds(c);
				c.getHighlight().paint(gc, bounds.x, bounds.y, bounds.width, bounds.height);
			});
		} finally {
			gc.dispose();
		}
	}

	protected GridCell getCellAt(int x, int y) {
		Point size = getSize();
		int cellWidth = ((size.x-headerSize) / columns) - (hSpacing);
		int cellHeight = ((size.y-headerSize) / rows) - (vSpacing);

		x -= headerSize;
		y -= headerSize;

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

	public Rectangle calculateBounds(GridCell cell) {
		Point size = getSize();
		int cellWidth = ((size.x-headerSize) / columns) - (hSpacing);
		int cellHeight = ((size.y-headerSize) / rows) - (vSpacing);

		int row = cell.getRow();
		int col = cell.getColumn();
		int x = headerSize+(col*(cellWidth+hSpacing));
		int y = headerSize+(row*(cellHeight+vSpacing));

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
		return new Rectangle(x,y,cellWidth,cellHeight);
	}

	public Point getCurrentCellSize() {
		Point size = getSize();
		int cellWidth = ((size.x-headerSize) / columns) - (hSpacing);
		int cellHeight = ((size.y-headerSize) / rows) - (vSpacing);
		return new Point(cellWidth, cellHeight);
	}

	public void redraw(GridCell cell) {
		// Unlike redraw(), this renders only the specified GridCell.
		if (cell != null && !isDisposed()) {
			Rectangle bounds = calculateBounds(cell);
			cellRenderer.prerender(this);
			GC gc = new GC(this);
			cell.paint(gc, bounds.x,bounds.y,bounds.width,bounds.height);
			gc.dispose();
		}
	}

}
