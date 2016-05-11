package eu.openanalytics.phaedra.base.ui.nattable.configuration.style;

import org.eclipse.nebula.widgets.nattable.config.AbstractRegistryConfiguration;
import org.eclipse.nebula.widgets.nattable.config.CellConfigAttributes;
import org.eclipse.nebula.widgets.nattable.config.IConfigRegistry;
import org.eclipse.nebula.widgets.nattable.data.convert.DefaultDisplayConverter;
import org.eclipse.nebula.widgets.nattable.data.convert.IDisplayConverter;
import org.eclipse.nebula.widgets.nattable.painter.cell.ICellPainter;
import org.eclipse.nebula.widgets.nattable.style.BorderStyle;
import org.eclipse.nebula.widgets.nattable.style.CellStyleAttributes;
import org.eclipse.nebula.widgets.nattable.style.HorizontalAlignmentEnum;
import org.eclipse.nebula.widgets.nattable.style.Style;
import org.eclipse.nebula.widgets.nattable.style.VerticalAlignmentEnum;
import org.eclipse.nebula.widgets.nattable.util.GUIHelper;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;

import eu.openanalytics.phaedra.base.ui.nattable.painter.ImageTextPainter;

public class PhaedraNatTableStyleConfiguration extends AbstractRegistryConfiguration {

	private Color bgColor = GUIHelper.COLOR_WHITE;
	private Color fgColor = GUIHelper.COLOR_BLACK;
	private Color gradientBgColor = GUIHelper.COLOR_WHITE;
	private Color gradientFgColor = GUIHelper.COLOR_BLACK;
	private Font font = GUIHelper.DEFAULT_FONT;
	private HorizontalAlignmentEnum hAlign = HorizontalAlignmentEnum.LEFT;
	private VerticalAlignmentEnum vAlign = VerticalAlignmentEnum.MIDDLE;
	private BorderStyle borderStyle = null;

	private ICellPainter cellPainter = new ImageTextPainter();
	private IDisplayConverter displayConverter;

	public PhaedraNatTableStyleConfiguration() {
		this(new DefaultDisplayConverter());
	}

	public PhaedraNatTableStyleConfiguration(IDisplayConverter displayConverter) {
		this.displayConverter = displayConverter;
	}

	@Override
	public void configureRegistry(IConfigRegistry configRegistry) {
		configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_PAINTER, cellPainter);

		Style cellStyle = new Style();
		cellStyle.setAttributeValue(CellStyleAttributes.BACKGROUND_COLOR, bgColor);
		cellStyle.setAttributeValue(CellStyleAttributes.FOREGROUND_COLOR, fgColor);
		cellStyle.setAttributeValue(CellStyleAttributes.GRADIENT_BACKGROUND_COLOR, gradientBgColor);
		cellStyle.setAttributeValue(CellStyleAttributes.GRADIENT_FOREGROUND_COLOR, gradientFgColor);
		cellStyle.setAttributeValue(CellStyleAttributes.FONT, font);
		cellStyle.setAttributeValue(CellStyleAttributes.HORIZONTAL_ALIGNMENT, hAlign);
		cellStyle.setAttributeValue(CellStyleAttributes.VERTICAL_ALIGNMENT, vAlign);
		cellStyle.setAttributeValue(CellStyleAttributes.BORDER_STYLE, borderStyle);

		configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, cellStyle);

		configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, displayConverter);
	}

}