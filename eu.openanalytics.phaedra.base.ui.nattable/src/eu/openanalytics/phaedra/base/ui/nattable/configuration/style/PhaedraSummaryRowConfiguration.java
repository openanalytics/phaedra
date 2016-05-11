package eu.openanalytics.phaedra.base.ui.nattable.configuration.style;

import org.eclipse.nebula.widgets.nattable.config.AbstractRegistryConfiguration;
import org.eclipse.nebula.widgets.nattable.config.CellConfigAttributes;
import org.eclipse.nebula.widgets.nattable.config.IConfigRegistry;
import org.eclipse.nebula.widgets.nattable.data.convert.DefaultDisplayConverter;
import org.eclipse.nebula.widgets.nattable.extension.glazedlists.groupBy.GroupByDataLayer;
import org.eclipse.nebula.widgets.nattable.style.BorderStyle;
import org.eclipse.nebula.widgets.nattable.style.BorderStyle.LineStyleEnum;
import org.eclipse.nebula.widgets.nattable.style.CellStyleAttributes;
import org.eclipse.nebula.widgets.nattable.style.DisplayMode;
import org.eclipse.nebula.widgets.nattable.style.Style;
import org.eclipse.nebula.widgets.nattable.summaryrow.ISummaryProvider;
import org.eclipse.nebula.widgets.nattable.summaryrow.SummaryDisplayConverter;
import org.eclipse.nebula.widgets.nattable.summaryrow.SummaryRowConfigAttributes;
import org.eclipse.nebula.widgets.nattable.summaryrow.SummaryRowLayer;
import org.eclipse.nebula.widgets.nattable.util.GUIHelper;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;

public class PhaedraSummaryRowConfiguration extends
        AbstractRegistryConfiguration {

    public BorderStyle summaryRowBorderStyle = new BorderStyle(0,
            GUIHelper.COLOR_BLACK, LineStyleEnum.DOTTED);
    public Color summaryRowFgColor = GUIHelper.COLOR_BLACK;
    public Color summaryRowBgColor = GUIHelper.COLOR_WIDGET_BACKGROUND;
    public Font summaryRowFont = GUIHelper.getFont(new FontData("Verdana", 8, SWT.BOLD)); //$NON-NLS-1$

    @Override
    public void configureRegistry(IConfigRegistry configRegistry) {
        addSummaryRowStyleConfig(configRegistry);
        // Disabled because this will also display "..." for columns that do not support Summary.
        //addSummaryProviderConfig(configRegistry);
        //addSummaryRowDisplayConverter(configRegistry);
    }

    protected void addSummaryRowStyleConfig(IConfigRegistry configRegistry) {
        Style cellStyle = new Style();
        cellStyle.setAttributeValue(CellStyleAttributes.FONT, this.summaryRowFont);
        cellStyle.setAttributeValue(CellStyleAttributes.BACKGROUND_COLOR,
                this.summaryRowBgColor);
        cellStyle.setAttributeValue(CellStyleAttributes.FOREGROUND_COLOR,
                this.summaryRowFgColor);
        cellStyle.setAttributeValue(CellStyleAttributes.BORDER_STYLE,
                this.summaryRowBorderStyle);
        
        configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE
        		, cellStyle
                , DisplayMode.NORMAL,
                SummaryRowLayer.DEFAULT_SUMMARY_ROW_CONFIG_LABEL);
        configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE
        		, cellStyle
        		, DisplayMode.NORMAL
        		, GroupByDataLayer.GROUP_BY_OBJECT);
    }

    protected void addSummaryProviderConfig(IConfigRegistry configRegistry) {
        configRegistry.registerConfigAttribute(
                SummaryRowConfigAttributes.SUMMARY_PROVIDER,
                ISummaryProvider.DEFAULT, DisplayMode.NORMAL,
                SummaryRowLayer.DEFAULT_SUMMARY_ROW_CONFIG_LABEL);
    }

    /**
     * Add a specialized {@link DefaultDisplayConverter} that will show "..." if
     * there is no value to show in the summary row yet.
     */
    protected void addSummaryRowDisplayConverter(IConfigRegistry configRegistry) {
        configRegistry.registerConfigAttribute(
                CellConfigAttributes.DISPLAY_CONVERTER,
                new SummaryDisplayConverter(new DefaultDisplayConverter()),
                DisplayMode.NORMAL,
                SummaryRowLayer.DEFAULT_SUMMARY_ROW_CONFIG_LABEL);
    }
}
