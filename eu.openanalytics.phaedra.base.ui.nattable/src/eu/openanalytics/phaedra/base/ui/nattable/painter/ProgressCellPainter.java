package eu.openanalytics.phaedra.base.ui.nattable.painter;

import org.eclipse.nebula.widgets.nattable.config.IConfigRegistry;
import org.eclipse.nebula.widgets.nattable.layer.cell.ILayerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;

import eu.openanalytics.phaedra.base.util.misc.NumberUtils;

/**
 * Paints numerical values on a 'progress bar' background. By default,
 * the progress is relative to value 1.0. To provide a custom max, use the
 * constructor {@link #ProgressCellPainter(double)}
 */
public class ProgressCellPainter extends ImageTextPainter {

	private double max;

	private static Color background = new Color(null, 170, 255, 170);

	public ProgressCellPainter() {
		this(1.0d);
	}

	public ProgressCellPainter(double max) {
		this.max = max;
	}

	@Override
	public void paintCell(ILayerCell cell, GC gc, Rectangle adjustedCellBounds, IConfigRegistry configRegistry) {
		double value = 0.0d;
		Object valueObject = cell.getDataValue();
		if (valueObject instanceof Number) value = ((Number)valueObject).doubleValue();
		else if (valueObject instanceof String) value = Double.valueOf((String)valueObject);

		double percentage = value / max;
		if (percentage < 0) percentage = 0;
		if (percentage > 1) percentage = 1;

		// Background: original background
		paintBackground(cell, gc, adjustedCellBounds, configRegistry);

		// Background: progress bar
		int fillWidth = (int) (adjustedCellBounds.width * percentage);
		if (background == null) {
			gc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_DARK_CYAN));
		} else {
			gc.setBackground(background);
		}
		gc.fillRectangle(adjustedCellBounds.x, adjustedCellBounds.y + 1, fillWidth, adjustedCellBounds.height - 2);

		// Foreground: text label
		String text = NumberUtils.round(value, 2);
		gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_BLACK));
		int textHeight = gc.textExtent(text).y;
		gc.drawText(text, adjustedCellBounds.x + 4, adjustedCellBounds.y + (adjustedCellBounds.height/2) - textHeight/2, true);
	}
}
