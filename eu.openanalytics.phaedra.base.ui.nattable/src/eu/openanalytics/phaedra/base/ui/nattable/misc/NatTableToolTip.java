package eu.openanalytics.phaedra.base.ui.nattable.misc;

import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.config.CellConfigAttributes;
import org.eclipse.nebula.widgets.nattable.data.IColumnPropertyAccessor;
import org.eclipse.nebula.widgets.nattable.data.IRowDataProvider;
import org.eclipse.nebula.widgets.nattable.grid.GridRegion;
import org.eclipse.nebula.widgets.nattable.layer.cell.CellDisplayConversionUtils;
import org.eclipse.nebula.widgets.nattable.layer.cell.ILayerCell;
import org.eclipse.nebula.widgets.nattable.painter.cell.ICellPainter;
import org.eclipse.nebula.widgets.nattable.style.DisplayMode;
import org.eclipse.nebula.widgets.nattable.tooltip.NatTableContentTooltip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Resource;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;

/**
 * A tooltip for Nattable that shows a text or image.
 * The text or image is retrieved using the following rules:
 * <ol>
 * <li>If the cell's data value is an {@link Image} or {@link ImageData}, that image is shown in the tooltip</li>
 * <li>If the column accessor is a {@link ITooltipColumnAccessor}, use it to obtain a text tooltip from the cell's row object</li>
 * <li>If the above did not yield a tooltip, attempt to convert the cell's data value into a String representation</li>
 * </ol>
 * 
 * @param <T> The type of row objects used by the table. 
 */
public class NatTableToolTip<T> extends NatTableContentTooltip {

	private IColumnPropertyAccessor<T> columnAccessor;
	private IRowDataProvider<T> dataProvider;

	private Resource resource;

	public NatTableToolTip(NatTable natTable, IColumnPropertyAccessor<T> columnAccessor,
			IRowDataProvider<T> dataProvider, String... tooltipRegions) {

		super(natTable, tooltipRegions);
		this.columnAccessor = columnAccessor;
		this.dataProvider = dataProvider;

		natTable.addListener(SWT.Dispose, e -> {
			if (resource != null) resource.dispose();
		});
	}

	@Override
	protected Image getImage(Event event) {
		ILayerCell cell = getCell(event);

		if (cell != null) {
			Object dataValue = cell.getDataValue();
			if (dataValue instanceof ImageData) {
				if (resource != null) resource.dispose();
				Image image = new Image(null, (ImageData) dataValue);
				resource = image;
				return image;
			}
			if (dataValue instanceof Image) {
				return (Image) dataValue;
			}
		}

		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected String getText(Event event) {
		ILayerCell cell = getCell(event);

		if (cell != null) {
			// If the registered cell painter is the PasswordCellPainter, there will be no tooltip
			ICellPainter painter = natTable.getConfigRegistry().getConfigAttribute(
					CellConfigAttributes.CELL_PAINTER, DisplayMode.NORMAL, cell.getConfigLabels().getLabels());
			if (!isVisibleContentPainter(painter)) return null;

			// First, try to use ITooltipColumnAccessor
			String tooltipValue = null;
			if (!cell.getConfigLabels().hasLabel(GridRegion.FILTER_ROW) && dataProvider != null
					&& columnAccessor instanceof ITooltipColumnAccessor) {

				T rowObject = null;
				if (cell.getOriginRowPosition() > 0) rowObject = dataProvider.getRowObject(cell.getRowIndex());
				tooltipValue = ((ITooltipColumnAccessor<T>)columnAccessor).getTooltipText(rowObject, cell.getColumnIndex());
			}

			// Else, try to convert the cell data value
			if (tooltipValue == null) {
				tooltipValue = CellDisplayConversionUtils.convertDataType(cell, natTable.getConfigRegistry());

				// Add the raw value if it differs from the converted value.
				Object value = cell.getDataValue();
				if (cell.getConfigLabels().hasLabel(GridRegion.BODY) && value != null) {
					String tooltipDataValue = value.toString();
					if (!tooltipValue.equalsIgnoreCase(tooltipDataValue)) {
						tooltipValue += "\n\n" + tooltipDataValue;
					}
				}
			}

			if (tooltipValue.length() > 0) return tooltipValue;
		}
		return null;
	}

	@Override
	protected Composite createToolTipContentArea(Event event, Composite parent) {
		CLabel label = (CLabel) super.createToolTipContentArea(event, parent);
		if (label.getImage() != null) label.setText(null);
		return label;
	}

	private ILayerCell getCell(Event event) {
		int col = natTable.getColumnPositionByX(event.x);
		int row = natTable.getRowPositionByY(event.y);
		ILayerCell cell = natTable.getCellByPosition(col, row);
		return cell;
	}

	public static interface ITooltipColumnAccessor<T> {
		/**
		 * <p>Get a customized tooltip for given row object and column.</p>
		 * <p>rowObject <code>null</code> is used for the column header tooltip.</p>
		 */
		public String getTooltipText(T rowObject, int colIndex);
	}
}