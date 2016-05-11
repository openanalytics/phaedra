package eu.openanalytics.phaedra.ui.subwell;

import java.util.List;

import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.data.IRowDataProvider;
import org.eclipse.nebula.widgets.nattable.layer.LabelStack;
import org.eclipse.nebula.widgets.nattable.layer.cell.ILayerCell;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Composite;

import eu.openanalytics.phaedra.base.ui.nattable.misc.AdvancedNatTableToolTip;
import eu.openanalytics.phaedra.base.ui.nattable.misc.NatTableToolTip.ITooltipColumnAccessor;
import eu.openanalytics.phaedra.base.ui.util.tooltip.IToolTipUpdate;
import eu.openanalytics.phaedra.model.subwell.SubWellItem;
import eu.openanalytics.phaedra.ui.wellimage.tooltip.SubWellToolTipLabelProvider;

public class SubWellNatTableToolTip extends AdvancedNatTableToolTip {

	public SubWellNatTableToolTip(NatTable table, ITooltipColumnAccessor<SubWellItem> columnAccessor
			, IRowDataProvider<SubWellItem> rowDataProvider) {

		super(table);

		setLabelProvider(new SubWellToolTipLabelProvider() {
			@Override
			public Image getImage(Object element) {
				ILayerCell cell = (ILayerCell) element;
				Object dataValue = cell.getDataValue();

				if (isImageObject(dataValue)) {
					return super.getImage(rowDataProvider.getRowObject(cell.getRowIndex()));
				}

				return null;
			}

			@Override
			public String getText(Object element) {
				ILayerCell cell = (ILayerCell) element;
				LabelStack cellConfigLabels = cell.getConfigLabels();
				Object dataValue = cell.getDataValue();

				String text = null;
				if (isFilterCellOrGroupByObject(cellConfigLabels)) {
					// Should get the default tooltip.
				} else if (isBodyCell(cellConfigLabels)) {
					if (isImageObject(dataValue)) {
						return super.getText(rowDataProvider.getRowObject(cell.getRowIndex()));
					} else {
						SubWellItem rowObject = rowDataProvider.getRowObject(cell.getRowIndex());
						text = columnAccessor.getTooltipText(rowObject, cell.getColumnIndex());
					}
				} else if (isColumnCell(cellConfigLabels)) {
					text = columnAccessor.getTooltipText(null, cell.getColumnIndex());
				}

				if (text == null) {
					text = getDefaultTextTooltip(cell);
				}

				return text;
			}

			@Override
			public void fillAdvancedControls(Composite parent, Object element, IToolTipUpdate update) {
				ILayerCell cell = (ILayerCell) element;
				Object dataValue = cell.getDataValue();
				if (isImageObject(dataValue)) {
					Object o = rowDataProvider.getRowObject(cell.getRowIndex());
					super.fillAdvancedControls(parent, o, update);
				}
			}
		});
	}

	public SubWellNatTableToolTip(NatTable table, ITooltipColumnAccessor<Integer> columnAccessor,
			List<SubWellItem> swItems, IRowDataProvider<Integer> rowDataProvider) {

		super(table);

		setLabelProvider(new SubWellToolTipLabelProvider() {
			@Override
			public Image getImage(Object element) {
				ILayerCell cell = (ILayerCell) element;
				Object dataValue = cell.getDataValue();

				if (isImageObject(dataValue)) {
					Integer rowObject = rowDataProvider.getRowObject(cell.getRowIndex());
					return super.getImage(swItems.get(rowObject));
				}

				return null;
			}

			@Override
			public String getText(Object element) {
				ILayerCell cell = (ILayerCell) element;
				LabelStack cellConfigLabels = cell.getConfigLabels();
				Object dataValue = cell.getDataValue();

				String text = null;
				if (isFilterCellOrGroupByObject(cellConfigLabels)) {
					// Should get the default tooltip.
				} else if (isBodyCell(cellConfigLabels)) {
					if (isImageObject(dataValue)) {
						Integer rowObject = rowDataProvider.getRowObject(cell.getRowIndex());
						text = super.getText(swItems.get(rowObject));
					} else {
						Integer rowObject = rowDataProvider.getRowObject(cell.getRowIndex());
						text = columnAccessor.getTooltipText(rowObject, cell.getColumnIndex());
					}
				} else if (isColumnCell(cellConfigLabels)) {
					text = columnAccessor.getTooltipText(null, cell.getColumnIndex());
				}
				if (text == null) {
					text = getDefaultTextTooltip(cell);
				}

				return text;
			}

			@Override
			public void fillAdvancedControls(Composite parent, Object element, IToolTipUpdate update) {
				ILayerCell cell = (ILayerCell) element;
				Object dataValue = cell.getDataValue();

				if (dataValue instanceof ImageData || dataValue instanceof Image) {
					Integer rowObject = rowDataProvider.getRowObject(cell.getRowIndex());
					super.fillAdvancedControls(parent, swItems.get(rowObject), update);
				}
			}
		});
	}

}
