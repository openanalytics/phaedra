package eu.openanalytics.phaedra.base.ui.gridviewer.widget;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;

import eu.openanalytics.phaedra.base.util.misc.NumberUtils;

public class GridHeaderCell extends GridCell {

	public static enum Type {
		Horizontal,
		Vertical
	}
	
	private static Color bgColor = new Color(null, 99, 99, 99);
	
	private Type type;
	
	public GridHeaderCell(Grid grid, int row, int column, Type type) {
		super(grid, row, column);
		this.type = type;
	}

	public Type getType() {
		return type;
	}
	
	public void paint(GC gc, int x, int y, int w, int h) {
		if (w == 0 || h == 0) return;
		
		gc.setBackground(bgColor);
		gc.fillRectangle(x,y,w+1,h+1);
		gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_WHITE));
		gc.drawText(getHeaderLabel(), x+2, y+2, true);
	}
	
	private String getHeaderLabel() {
		int index = getColumn() + 1;
		String label = "" + index;
		if (type == Type.Vertical) label = NumberUtils.getWellRowLabel(index);
		return label;
	}
}
