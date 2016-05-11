package eu.openanalytics.phaedra.base.ui.nattable.configuration.style;

import org.eclipse.nebula.widgets.nattable.config.AbstractRegistryConfiguration;
import org.eclipse.nebula.widgets.nattable.config.CellConfigAttributes;
import org.eclipse.nebula.widgets.nattable.config.IConfigRegistry;
import org.eclipse.nebula.widgets.nattable.filterrow.FilterIconPainter;
import org.eclipse.nebula.widgets.nattable.filterrow.FilterRowPainter;
import org.eclipse.nebula.widgets.nattable.filterrow.TextMatchingMode;
import org.eclipse.nebula.widgets.nattable.filterrow.config.FilterRowConfigAttributes;
import org.eclipse.nebula.widgets.nattable.grid.GridRegion;
import org.eclipse.nebula.widgets.nattable.style.CellStyleAttributes;
import org.eclipse.nebula.widgets.nattable.style.DisplayMode;
import org.eclipse.nebula.widgets.nattable.style.Style;
import org.eclipse.nebula.widgets.nattable.util.GUIHelper;

import eu.openanalytics.phaedra.base.ui.nattable.misc.DefaultAdvancedComparator;

public class PhaedraFilterRowConfiguration extends AbstractRegistryConfiguration {

	@Override
	public void configureRegistry(IConfigRegistry configRegistry) {
		// Override the default filter row configuration for painter
		configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_PAINTER,
				new FilterRowPainter(new FilterIconPainter(GUIHelper.getImage("filter"))),
				DisplayMode.NORMAL,
				GridRegion.FILTER_ROW
		);

		// The character used for separating the filter into multiple ones.
		configRegistry.registerConfigAttribute(
				FilterRowConfigAttributes.TEXT_DELIMITER
				, ", "
				, DisplayMode.NORMAL
		);

		// Use regular expressions which support the following actions: <>, =, <(=) and >(=)
		configRegistry.registerConfigAttribute(
				FilterRowConfigAttributes.TEXT_MATCHING_MODE
				, TextMatchingMode.REGULAR_EXPRESSION
		);

		// Default comparator. Used to compare objects in the column during threshold matching.
		configRegistry.registerConfigAttribute(
				FilterRowConfigAttributes.FILTER_COMPARATOR
				, DefaultAdvancedComparator.getInstance()
		);

		final Style rowStyle = new Style();
		rowStyle.setAttributeValue(CellStyleAttributes.BACKGROUND_COLOR, GUIHelper.COLOR_WHITE);
		configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, rowStyle, DisplayMode.NORMAL, GridRegion.FILTER_ROW);
	}

}