package eu.openanalytics.phaedra.base.ui.nattable.configuration.style;

import org.eclipse.nebula.widgets.nattable.config.AbstractRegistryConfiguration;
import org.eclipse.nebula.widgets.nattable.config.CellConfigAttributes;
import org.eclipse.nebula.widgets.nattable.config.IConfigRegistry;
import org.eclipse.nebula.widgets.nattable.grid.GridRegion;
import org.eclipse.nebula.widgets.nattable.style.BorderStyle;
import org.eclipse.nebula.widgets.nattable.style.BorderStyle.LineStyleEnum;
import org.eclipse.nebula.widgets.nattable.style.CellStyleAttributes;
import org.eclipse.nebula.widgets.nattable.style.DisplayMode;
import org.eclipse.nebula.widgets.nattable.style.SelectionStyleLabels;
import org.eclipse.nebula.widgets.nattable.style.Style;
import org.eclipse.nebula.widgets.nattable.util.GUIHelper;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;

public class PhaedraSelectionStyleConfiguration extends AbstractRegistryConfiguration {

	// Selection style
	//private Font selectionFont = GUIHelper.DEFAULT_FONT;
	private Color selectionBgColor = GUIHelper.getColor(200, 230, 255);
	private Color selectionFgColor = GUIHelper.COLOR_BLACK;

	// Anchor style
	private Color anchorBorderColor = GUIHelper.COLOR_DARK_GRAY;
	private BorderStyle anchorBorderStyle = new BorderStyle(1, anchorBorderColor, LineStyleEnum.SOLID);
	private Color anchorBgColor = GUIHelper.getColor(200, 230, 255);
	private Color anchorFgColor = GUIHelper.COLOR_BLACK;

	// Selected headers style
	private Color selectedHeaderBgColor = GUIHelper.getColor(230, 230, 100);
	private Color selectedHeaderFgColor = GUIHelper.COLOR_BLACK;
	private Font selectedHeaderFont = GUIHelper.DEFAULT_FONT;
	private BorderStyle selectedHeaderBorderStyle = new BorderStyle(1, GUIHelper.COLOR_WIDGET_NORMAL_SHADOW,
			LineStyleEnum.SOLID);

	@Override
	public void configureRegistry(IConfigRegistry configRegistry) {
		configureSelectionStyle(configRegistry);
		configureSelectionAnchorStyle(configRegistry);
		configureHeaderHasSelectionStyle(configRegistry);
		configureHeaderFullySelectedStyle(configRegistry);
	}

	protected void configureSelectionStyle(IConfigRegistry configRegistry) {
		Style cellStyle = new Style();
		// Overwrites custom font layout on selection, unwanted behavior.
		//cellStyle.setAttributeValue(CellStyleAttributes.FONT, selectionFont);
		cellStyle.setAttributeValue(CellStyleAttributes.BACKGROUND_COLOR, selectionBgColor);
		cellStyle.setAttributeValue(CellStyleAttributes.FOREGROUND_COLOR, selectionFgColor);
		configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, cellStyle, DisplayMode.SELECT);
	}

	private void configureSelectionAnchorStyle(IConfigRegistry configRegistry) {
		// Selection anchor style for normal display mode
		Style cellStyle = new Style();
		cellStyle.setAttributeValue(CellStyleAttributes.BORDER_STYLE, anchorBorderStyle);
		configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, cellStyle, DisplayMode.NORMAL, SelectionStyleLabels.SELECTION_ANCHOR_STYLE);

		// Selection anchor style for select display mode
		cellStyle = new Style();
		cellStyle.setAttributeValue(CellStyleAttributes.BACKGROUND_COLOR, anchorBgColor);
		cellStyle.setAttributeValue(CellStyleAttributes.FOREGROUND_COLOR, anchorFgColor);
		cellStyle.setAttributeValue(CellStyleAttributes.BORDER_STYLE, anchorBorderStyle);
		configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, cellStyle, DisplayMode.SELECT, SelectionStyleLabels.SELECTION_ANCHOR_STYLE);
	}

	private void configureHeaderHasSelectionStyle(IConfigRegistry configRegistry) {
		Style cellStyle = new Style();

		cellStyle.setAttributeValue(CellStyleAttributes.FOREGROUND_COLOR, selectedHeaderFgColor);
		cellStyle.setAttributeValue(CellStyleAttributes.BACKGROUND_COLOR, selectedHeaderBgColor);
		cellStyle.setAttributeValue(CellStyleAttributes.FONT, selectedHeaderFont);
		cellStyle.setAttributeValue(CellStyleAttributes.BORDER_STYLE, selectedHeaderBorderStyle);

		configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, cellStyle, DisplayMode.SELECT, GridRegion.COLUMN_HEADER);
		configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, cellStyle, DisplayMode.SELECT, GridRegion.CORNER);
		configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, cellStyle, DisplayMode.SELECT, GridRegion.ROW_HEADER);
	}

	private void configureHeaderFullySelectedStyle(IConfigRegistry configRegistry) {
		// Header fully selected
		Style cellStyle = new Style();
		cellStyle.setAttributeValue(CellStyleAttributes.BACKGROUND_COLOR, selectedHeaderBgColor);

		configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, cellStyle, DisplayMode.SELECT, SelectionStyleLabels.COLUMN_FULLY_SELECTED_STYLE);
		configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, cellStyle, DisplayMode.SELECT, SelectionStyleLabels.ROW_FULLY_SELECTED_STYLE);
	}

}