package eu.openanalytics.phaedra.base.ui.gridviewer.widget;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;

import eu.openanalytics.phaedra.base.ui.gridviewer.widget.render.IGridCellRenderer;
import eu.openanalytics.phaedra.base.ui.util.highlighter.Highlight;

public class GridCell {

	private Grid grid;
	private int row;
	private int column;

	private Object data;

	private Highlight highlight;

	public GridCell(Grid grid, int row, int column) {
		this.grid = grid;
		this.row = row;
		this.column = column;

		this.highlight = new Highlight();
	}

	public void paint(GC gc, int x, int y, int w, int h) {
		IGridCellRenderer cellRenderer = grid.getCellRenderer();
		if (cellRenderer != null) {
			Rectangle clip = gc.getClipping();
			gc.setClipping(x, y, w+1, h+1);
			cellRenderer.render(this, gc, x, y, w, h);
			gc.setClipping(clip);
		}

		highlight.paint(gc, x, y, w, h);
	}

	public String getTooltip() {
		IGridCellRenderer cellRenderer = grid.getCellRenderer();
		if (cellRenderer != null) {
			return cellRenderer.getTooltipContribution(this);
		}
		return "";
	}

	public int getRow() {
		return row;
	}

	public int getColumn() {
		return column;
	}

	public Object getData() {
		return data;
	}

	public void setData(Object data) {
		this.data = data;
	}

	public Grid getGrid() {
		return grid;
	}

	public Highlight getHighlight() {
		return highlight;
	}
}
