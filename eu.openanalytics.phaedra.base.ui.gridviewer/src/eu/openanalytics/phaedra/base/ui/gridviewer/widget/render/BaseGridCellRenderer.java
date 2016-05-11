package eu.openanalytics.phaedra.base.ui.gridviewer.widget.render;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

import eu.openanalytics.phaedra.base.ui.gridviewer.widget.Grid;
import eu.openanalytics.phaedra.base.ui.gridviewer.widget.GridCell;

/**
 * A base implementation that renders nothing.
 * To render something, override one of the get* methods.
 */
public abstract class BaseGridCellRenderer implements IGridCellRenderer {
	
	public Image getImage(GridCell cell) {
		return null;
	}
	
	public Color getBgColor(GridCell cell) {
		return null;
	}
	
	public String[] getLabels(GridCell cell) {
		return null;
	}
	
	public Font getFont(GridCell cell) {
		return null;
	}
	
	@Override
	public String getTooltipContribution(GridCell cell) {
		// By default there is no tooltip.
		return null;
	}
	
	@Override
	public void prerender(Grid grid) {
		// Nothing to prerender
	}
	
	@Override
	public void render(GridCell cell, GC gc, int x, int y, int w, int h) {
		
		Color bgColor = getBgColor(cell);
		if (bgColor != null) {
			gc.setBackground(bgColor);
			gc.fillRectangle(gc.getClipping());
		}
		
		Image image = getImage(cell);
		if (image != null && !image.isDisposed()) {
			gc.drawImage(image, 0, 0);
		}
		
		String[] labels = getLabels(cell);
		if (labels != null  && labels.length > 0) {
			// Use a custom font, if provided.
			Font font = getFont(cell);
			if (font != null) gc.setFont(font);

			// Remove invalid (empty or null) labels.
			int labelCount = 0;
			String[] validLabels = new String[labels.length];
			for (int i=0; i<labels.length; i++) {
				if (labels[i] != null && !labels[i].isEmpty()) {
					validLabels[labelCount++] = labels[i];
				}
			}
			if (labelCount == 0) return;
			
			// Calculate space needed per label.
			Point[] labelSizes = new Point[labelCount];
			for (int i=0; i<labelCount; i++) {
				labelSizes[i] = gc.textExtent(validLabels[i]);
				// Text extent is a bit too conservative. Clip a few pixels.
				labelSizes[i].y = labelSizes[i].y - 3;
			}
			int heightPerLabel = labelSizes[0].y;

			int labelsToShow = labelCount;
			// If there is not enough space to show all labels: show as many as possible.
			if (heightPerLabel*labelCount > h) {
				labelsToShow = h / heightPerLabel;
			}
			if (labelsToShow == 0) return;
			
			int availableHeightPerLabel = h / labelsToShow;
			for (int i=0; i<labelCount; i++) {
				String lbl = validLabels[i];
				Point p = labelSizes[i];
				int offsetX = Math.max(0, (w-p.x)/2);
				int offsetY = Math.max(0, (i*availableHeightPerLabel)+((availableHeightPerLabel-p.y)/2));
				gc.drawText(lbl, x+offsetX, y+offsetY, true);
			}
		}
	}
	
	@Override
	public void dispose() {
		// Nothing to dispose.
	}
}
