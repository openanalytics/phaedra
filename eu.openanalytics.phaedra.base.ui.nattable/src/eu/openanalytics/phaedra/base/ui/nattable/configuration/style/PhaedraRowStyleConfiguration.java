package eu.openanalytics.phaedra.base.ui.nattable.configuration.style;

import org.eclipse.nebula.widgets.nattable.config.AbstractRegistryConfiguration;
import org.eclipse.nebula.widgets.nattable.config.CellConfigAttributes;
import org.eclipse.nebula.widgets.nattable.config.IConfigRegistry;
import org.eclipse.nebula.widgets.nattable.grid.cell.AlternatingRowConfigLabelAccumulator;
import org.eclipse.nebula.widgets.nattable.style.CellStyleAttributes;
import org.eclipse.nebula.widgets.nattable.style.DisplayMode;
import org.eclipse.nebula.widgets.nattable.style.Style;
import org.eclipse.nebula.widgets.nattable.util.GUIHelper;
import org.eclipse.swt.graphics.Color;

public class PhaedraRowStyleConfiguration extends AbstractRegistryConfiguration {

	private Color evenRowBgColor = GUIHelper.COLOR_WIDGET_BACKGROUND;
	private Color oddRowBgColor = GUIHelper.COLOR_WHITE;
	
	private boolean useOneColor;

	public PhaedraRowStyleConfiguration() {
		this.useOneColor = true;
	}
	
	public PhaedraRowStyleConfiguration(boolean useOneColor) {
		this.useOneColor = useOneColor;
	}
	
	@Override
	public void configureRegistry(IConfigRegistry configRegistry) {
		configureOddRowStyle(configRegistry);
		configureEvenRowStyle(configRegistry);
	}

	private void configureOddRowStyle(IConfigRegistry configRegistry) {
		Style cellStyle = new Style();
		cellStyle.setAttributeValue(CellStyleAttributes.BACKGROUND_COLOR, oddRowBgColor);
		configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, cellStyle, DisplayMode.NORMAL, AlternatingRowConfigLabelAccumulator.EVEN_ROW_CONFIG_TYPE);
	}

	private void configureEvenRowStyle(IConfigRegistry configRegistry) {
		Style cellStyle = new Style();
		if (useOneColor) {
			cellStyle.setAttributeValue(CellStyleAttributes.BACKGROUND_COLOR, oddRowBgColor);
		} else {
			cellStyle.setAttributeValue(CellStyleAttributes.BACKGROUND_COLOR, evenRowBgColor);
		}
		configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, cellStyle, DisplayMode.NORMAL, AlternatingRowConfigLabelAccumulator.ODD_ROW_CONFIG_TYPE);
	}
	
}