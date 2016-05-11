package eu.openanalytics.phaedra.ui.plate.view;

import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.data.IRowDataProvider;
import org.eclipse.nebula.widgets.nattable.layer.LabelStack;
import org.eclipse.nebula.widgets.nattable.layer.cell.ILayerCell;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;

import eu.openanalytics.phaedra.base.ui.nattable.misc.AdvancedNatTableToolTip;
import eu.openanalytics.phaedra.base.ui.nattable.misc.NatTableToolTip.ITooltipColumnAccessor;
import eu.openanalytics.phaedra.base.ui.util.tooltip.IToolTipUpdate;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.ui.wellimage.tooltip.WellToolTipLabelProvider;

public class WellNatTableToolTip extends AdvancedNatTableToolTip {

	public WellNatTableToolTip(NatTable table, ITooltipColumnAccessor<Well> columnAccessor, IRowDataProvider<Well> rowDataProvider) {
		super(table);

		setLabelProvider(new WellToolTipLabelProvider() {
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
						text = super.getText(rowDataProvider.getRowObject(cell.getRowIndex()));
					} else {
						Well rowObject = rowDataProvider.getRowObject(cell.getRowIndex());
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
				if (cell == null) return;
				Object dataValue = cell.getDataValue();
				if (isImageObject(dataValue)) {
					Object o = rowDataProvider.getRowObject(cell.getRowIndex());
					super.fillAdvancedControls(parent, o, update);
				}
			}
		});
	}

}
