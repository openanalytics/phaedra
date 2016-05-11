package eu.openanalytics.phaedra.base.ui.nattable.configuration.style;

import org.eclipse.nebula.widgets.nattable.config.AbstractRegistryConfiguration;
import org.eclipse.nebula.widgets.nattable.config.CellConfigAttributes;
import org.eclipse.nebula.widgets.nattable.config.IConfigRegistry;
import org.eclipse.nebula.widgets.nattable.grid.GridRegion;
import org.eclipse.nebula.widgets.nattable.painter.cell.ICellPainter;
import org.eclipse.nebula.widgets.nattable.painter.cell.TextPainter;
import org.eclipse.nebula.widgets.nattable.style.BorderStyle;
import org.eclipse.nebula.widgets.nattable.style.BorderStyle.LineStyleEnum;
import org.eclipse.nebula.widgets.nattable.style.CellStyleAttributes;
import org.eclipse.nebula.widgets.nattable.style.DisplayMode;
import org.eclipse.nebula.widgets.nattable.style.HorizontalAlignmentEnum;
import org.eclipse.nebula.widgets.nattable.style.Style;
import org.eclipse.nebula.widgets.nattable.style.VerticalAlignmentEnum;
import org.eclipse.nebula.widgets.nattable.util.GUIHelper;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;

public class PhaedraRowHeaderStyleConfiguration extends AbstractRegistryConfiguration {

	private Font font = GUIHelper.DEFAULT_FONT;
	private Color bgColor = GUIHelper.COLOR_WIDGET_BACKGROUND;
	private Color fgColor = GUIHelper.COLOR_BLACK;
	private Color gradientBgColor = GUIHelper.COLOR_WIDGET_BACKGROUND;
	private Color gradientFgColor = GUIHelper.COLOR_WIDGET_NORMAL_SHADOW;
	private HorizontalAlignmentEnum hAlign = HorizontalAlignmentEnum.CENTER;
	private VerticalAlignmentEnum vAlign = VerticalAlignmentEnum.MIDDLE;
	private BorderStyle borderStyle = new BorderStyle(1, GUIHelper.COLOR_WIDGET_NORMAL_SHADOW, LineStyleEnum.SOLID);

	private ICellPainter cellPainter = new TextPainter();

	@Override
	public void configureRegistry(IConfigRegistry configRegistry) {
		configureRowHeaderCellPainter(configRegistry);
		configureRowHeaderStyle(configRegistry);
	}

	private void configureRowHeaderStyle(IConfigRegistry configRegistry) {
		Style cellStyle = new Style();
		cellStyle.setAttributeValue(CellStyleAttributes.BACKGROUND_COLOR, bgColor);
		cellStyle.setAttributeValue(CellStyleAttributes.FOREGROUND_COLOR, fgColor);
		cellStyle.setAttributeValue(CellStyleAttributes.GRADIENT_BACKGROUND_COLOR, gradientBgColor);
		cellStyle.setAttributeValue(CellStyleAttributes.GRADIENT_FOREGROUND_COLOR, gradientFgColor);
		cellStyle.setAttributeValue(CellStyleAttributes.HORIZONTAL_ALIGNMENT, hAlign);
		cellStyle.setAttributeValue(CellStyleAttributes.VERTICAL_ALIGNMENT, vAlign);
		cellStyle.setAttributeValue(CellStyleAttributes.BORDER_STYLE, borderStyle);
		cellStyle.setAttributeValue(CellStyleAttributes.FONT, font);

		configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, cellStyle, DisplayMode.NORMAL, GridRegion.ROW_HEADER);
	}

	private void configureRowHeaderCellPainter(IConfigRegistry configRegistry) {
		configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_PAINTER, cellPainter, DisplayMode.NORMAL, GridRegion.ROW_HEADER);
	}

}