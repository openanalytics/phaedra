package eu.openanalytics.phaedra.base.ui.nattable.painter;

import org.eclipse.nebula.widgets.nattable.config.IConfigRegistry;
import org.eclipse.nebula.widgets.nattable.layer.cell.ILayerCell;
import org.eclipse.nebula.widgets.nattable.painter.cell.CellPainterWrapper;
import org.eclipse.nebula.widgets.nattable.painter.cell.ICellPainter;
import org.eclipse.nebula.widgets.nattable.style.BorderStyle;
import org.eclipse.nebula.widgets.nattable.style.CellStyleAttributes;
import org.eclipse.nebula.widgets.nattable.style.CellStyleUtil;
import org.eclipse.nebula.widgets.nattable.style.IStyle;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;

public class CustomBorderLineDecorator extends CellPainterWrapper {

	public CustomBorderLineDecorator(ICellPainter painter) {
		super(painter);
	}

	@Override
	public void paintCell(ILayerCell cell, GC gc, Rectangle bounds, IConfigRegistry configRegistry) {
		BorderStyle borderStyle = getBorderStyle(cell, configRegistry);

		if (borderStyle != null) {
			gc.setForeground(borderStyle.getColor());
			if (cell.getRowPosition() == 0) gc.drawLine(bounds.x, bounds.y, bounds.x + bounds.width - 1, bounds.y);
			if (cell.getColumnPosition() == 0) gc.drawLine(bounds.x, bounds.y, bounds.x, bounds.y + bounds.height - 1);
			gc.drawLine(bounds.x, bounds.y + bounds.height - 1, bounds.x + bounds.width - 1, bounds.y + bounds.height - 1);
			gc.drawLine(bounds.x + bounds.width - 1, bounds.y, bounds.x + bounds.width - 1, bounds.y + bounds.height - 1);

			super.paintCell(cell, gc, new Rectangle(bounds.x + 1, bounds.y + 1, bounds.width - 2, bounds.height - 2), configRegistry);
		} else {
			super.paintCell(cell, gc, bounds, configRegistry);
		}
	}

	@Override
	public int getPreferredHeight(ILayerCell cell, GC gc, IConfigRegistry configRegistry) {
		int prefHeight = super.getPreferredHeight(cell, gc, configRegistry);
		return prefHeight + 2;
	}

	@Override
	public int getPreferredWidth(ILayerCell cell, GC gc, IConfigRegistry configRegistry) {
		int prefWidth = super.getPreferredWidth(cell, gc, configRegistry);
		return prefWidth + 2;
	}

	private BorderStyle getBorderStyle(ILayerCell cell, IConfigRegistry configRegistry) {
		IStyle cellStyle = CellStyleUtil.getCellStyle(cell, configRegistry);
		BorderStyle borderStyle = cellStyle.getAttributeValue(CellStyleAttributes.BORDER_STYLE);
		return borderStyle;
	}

}