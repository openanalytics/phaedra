package eu.openanalytics.phaedra.base.ui.nattable.painter;

import org.eclipse.nebula.widgets.nattable.config.IConfigRegistry;
import org.eclipse.nebula.widgets.nattable.layer.DataLayer;
import org.eclipse.nebula.widgets.nattable.layer.ILayer;
import org.eclipse.nebula.widgets.nattable.layer.cell.ILayerCell;
import org.eclipse.nebula.widgets.nattable.painter.cell.ICellPainter;
import org.eclipse.nebula.widgets.nattable.painter.cell.TextPainter;
import org.eclipse.nebula.widgets.nattable.resize.command.ColumnResizeCommand;
import org.eclipse.nebula.widgets.nattable.resize.command.RowResizeCommand;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Rectangle;

/**
 * Paints a text or an image on a cell.
 * <ul>
 * <li>If the image is larger than the cell, the cell is resized automatically.</li>
 * <li>If the image is smaller than the cell, the image is centered in the cell.</li>
 * </ul>
 * For more information on how text is painted, see {@link TextPainter}.
 */
public class ImageTextPainter extends TextPainter {

	private static final int BORDER_WIDTH = 1;

	public ImageTextPainter() {
		super(false, true, 3, false);
	}

	@Override
	public void paintCell(ILayerCell cell, GC gc, Rectangle rectangle, IConfigRegistry configRegistry) {
		Object dataValue = cell.getDataValue();
		if (dataValue instanceof ImageData) {
			Rectangle originalClipping = gc.getClipping();
			gc.setClipping(rectangle.intersection(originalClipping));
			paintBackground(cell, gc, rectangle, configRegistry);
			checkAvailableSize(cell, gc, rectangle, configRegistry);
			
			Image img = new Image(null, (ImageData) dataValue);
			try {
				paintImage(gc, rectangle, img);
			} finally {
				img.dispose();
			}	

			gc.setClipping(originalClipping);
		} else if (dataValue instanceof Image) {
			Rectangle originalClipping = gc.getClipping();
			gc.setClipping(rectangle.intersection(originalClipping));
			paintBackground(cell, gc, rectangle, configRegistry);
			checkAvailableSize(cell, gc, rectangle, configRegistry);

			paintImage(gc, rectangle, (Image) dataValue);

			gc.setClipping(originalClipping);
		} else {
			super.paintCell(cell, gc, rectangle, configRegistry);
		}
	}

	@Override
	public int getPreferredWidth(ILayerCell cell, GC gc, IConfigRegistry configRegistry) {
		if (cell.getDataValue() instanceof ImageData) {
			ImageData image = (ImageData) cell.getDataValue();
			return image.width + BORDER_WIDTH;
		} else if (cell.getDataValue() instanceof Image) {
			Image image = (Image) cell.getDataValue();
			return image.getBounds().width + BORDER_WIDTH;
		}
		return super.getPreferredWidth(cell, gc, configRegistry);
	}

	@Override
	public int getPreferredHeight(ILayerCell cell, GC gc, IConfigRegistry configRegistry) {
		int height = DataLayer.DEFAULT_ROW_HEIGHT;
		if (cell.getDataValue() instanceof ImageData) {
			ImageData image = (ImageData) cell.getDataValue();
			height = Math.max(height, image.height + BORDER_WIDTH);
		} else if (cell.getDataValue() instanceof Image) {
			Image image = (Image) cell.getDataValue();
			height = Math.max(height, image.getBounds().height + BORDER_WIDTH);
		} else {
			height = super.getPreferredHeight(cell, gc, configRegistry);
		}
		return height;
	}

	protected void paintBackground(ILayerCell cell, GC gc, Rectangle rectangle, IConfigRegistry configRegistry) {
		if (paintBg) {
			Color backgroundColor = getBackgroundColour(cell, configRegistry);
			if (backgroundColor != null) {
				Color originalBackground = gc.getBackground();
				gc.setBackground(backgroundColor);
				gc.fillRectangle(rectangle);
				gc.setBackground(originalBackground);
			}
		}
	}

	private void paintImage(GC gc, Rectangle rectangle, Image image) {
		if (image.isDisposed()) return;
		Rectangle imgBounds = image.getBounds();
		gc.drawImage(
				image,
				rectangle.x + (rectangle.width - imgBounds.width) / 2,
				rectangle.y + (rectangle.height - imgBounds.height) / 2);
	}

	/**
	 * Check whether the cell's contents fit inside the current painting bounds.
	 * If not, the row and/or column are resized to accommodate the cell contents.
	 */
	private void checkAvailableSize(ILayerCell cell, GC gc, Rectangle rectangle, IConfigRegistry configRegistry) {
		int prefHeight = getPreferredHeight(cell, gc, configRegistry);
		int prefWidth = getPreferredWidth(cell, gc, configRegistry);
		ILayer layer = cell.getLayer();
		
		if (prefHeight > rectangle.height + BORDER_WIDTH) {
			layer.doCommand(new RowResizeCommand(layer, cell.getRowPosition(), prefHeight));
		}
		
		if (prefWidth > rectangle.width + BORDER_WIDTH) {
			ICellPainter painter = layer.getCellPainter(cell.getColumnPosition(), cell.getRowPosition(), cell, configRegistry);
			if (painter == null) return;
			prefWidth = painter.getPreferredWidth(cell, gc, configRegistry);
			layer.doCommand(new ColumnResizeCommand(layer, cell.getColumnPosition(), prefWidth));
		}
	}
}