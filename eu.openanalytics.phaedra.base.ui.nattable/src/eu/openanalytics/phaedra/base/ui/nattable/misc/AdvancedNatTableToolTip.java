package eu.openanalytics.phaedra.base.ui.nattable.misc;

import org.eclipse.jface.window.ToolTip;
import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.config.CellConfigAttributes;
import org.eclipse.nebula.widgets.nattable.extension.glazedlists.groupBy.GroupByDataLayer;
import org.eclipse.nebula.widgets.nattable.grid.GridRegion;
import org.eclipse.nebula.widgets.nattable.layer.LabelStack;
import org.eclipse.nebula.widgets.nattable.layer.cell.CellDisplayConversionUtils;
import org.eclipse.nebula.widgets.nattable.layer.cell.ILayerCell;
import org.eclipse.nebula.widgets.nattable.painter.cell.CellPainterWrapper;
import org.eclipse.nebula.widgets.nattable.painter.cell.ICellPainter;
import org.eclipse.nebula.widgets.nattable.painter.cell.PasswordTextPainter;
import org.eclipse.nebula.widgets.nattable.style.DisplayMode;
import org.eclipse.nebula.widgets.nattable.summaryrow.SummaryRowLayer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Event;

import eu.openanalytics.phaedra.base.ui.util.tooltip.AdvancedToolTip;
import eu.openanalytics.phaedra.base.ui.util.tooltip.ToolTipLabelProvider;

public class AdvancedNatTableToolTip extends AdvancedToolTip {

	private NatTable table;

	protected AdvancedNatTableToolTip(NatTable table) {
		super(table, ToolTip.NO_RECREATE, false);
		setPopupDelay(500);
		setShift(new Point(5, 5));

		this.table = table;
	}

	public AdvancedNatTableToolTip(NatTable table, ToolTipLabelProvider labelProvider) {
		this(table);

		setLabelProvider(labelProvider);
	}

	@Override
	public Object getData(Event event) {
		int col = table.getColumnPositionByX(event.x);
		int row = table.getRowPositionByY(event.y);
		return table.getCellByPosition(col, row);
	}

	protected boolean isVisibleContentPainter(ICellPainter painter) {
		if (painter instanceof PasswordTextPainter) {
			return false;
		} else if (painter instanceof CellPainterWrapper) {
			return isVisibleContentPainter(((CellPainterWrapper) painter).getWrappedPainter());
		}
		return true;
	}

	protected String getDefaultTextTooltip(ILayerCell cell) {
		String tooltipValue = null;
		Object dataValue = cell.getDataValue();
		LabelStack cellConfigLabels = cell.getConfigLabels();

		ICellPainter painter = table.getConfigRegistry().getConfigAttribute(
				CellConfigAttributes.CELL_PAINTER, DisplayMode.NORMAL, cellConfigLabels.getLabels());
		if (isVisibleContentPainter(painter)) {
			tooltipValue = CellDisplayConversionUtils.convertDataType(cell, table.getConfigRegistry());

			if ((isBodyCell(cellConfigLabels) || isSummaryCell(cellConfigLabels)) && dataValue != null) {
				// Add the raw value if it differs from the converted value.
				String extraTooltipValue = dataValue.toString();
				if (extraTooltipValue != null && !extraTooltipValue.isEmpty()) {
					if (tooltipValue == null || tooltipValue.isEmpty()) {
						tooltipValue = extraTooltipValue;
					} else if (!tooltipValue.equalsIgnoreCase(extraTooltipValue)) {
						tooltipValue += "\n\n" + extraTooltipValue;
					}
				}
			}
			if (tooltipValue != null && tooltipValue.isEmpty()) {
				tooltipValue = null;
			}
		}

		return tooltipValue;
	}

	protected boolean isFilterCellOrGroupByObject(LabelStack cellConfigLabels) {
		return cellConfigLabels.hasLabel(GroupByDataLayer.GROUP_BY_OBJECT) || cellConfigLabels.hasLabel(GridRegion.FILTER_ROW);
	}

	protected boolean isBodyCell(LabelStack cellConfigLabels) {
		return cellConfigLabels.hasLabel(GridRegion.BODY);
	}

	protected boolean isColumnCell(LabelStack cellConfigLabels) {
		return cellConfigLabels.hasLabel(GridRegion.COLUMN_HEADER);
	}

	protected boolean isSummaryCell(LabelStack cellConfigLabels) {
		return cellConfigLabels.hasLabel(SummaryRowLayer.DEFAULT_SUMMARY_ROW_CONFIG_LABEL);
	}

	protected boolean isImageObject(Object dataValue) {
		return dataValue instanceof ImageData || dataValue instanceof Image;
	}

}