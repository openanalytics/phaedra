package eu.openanalytics.phaedra.ui.curve.grid.tooltip;

import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.data.IRowDataProvider;
import org.eclipse.nebula.widgets.nattable.layer.LabelStack;
import org.eclipse.nebula.widgets.nattable.layer.cell.ILayerCell;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Composite;

import eu.openanalytics.phaedra.base.ui.nattable.misc.AdvancedNatTableToolTip;
import eu.openanalytics.phaedra.base.ui.util.tooltip.IToolTipUpdate;
import eu.openanalytics.phaedra.model.plate.vo.Compound;
import eu.openanalytics.phaedra.ui.curve.grid.provider.CompoundImageContentProvider;
import eu.openanalytics.phaedra.ui.wellimage.tooltip.WellToolTipLabelProvider;

public class CompoundImageToolTip extends AdvancedNatTableToolTip {

	public CompoundImageToolTip(NatTable table, CompoundImageContentProvider columnAccessor, IRowDataProvider<Compound> rowDataProvider) {
		super(table);

		setLabelProvider(new WellToolTipLabelProvider() {
			@Override
			public Image getImage(Object element) {
				ILayerCell cell = (ILayerCell) element;
				Object dataValue = cell.getDataValue();

				if (isImageObject(dataValue)) {
					Object o = getImageObject(columnAccessor, rowDataProvider, cell);
					if (!(o instanceof Compound)) return super.getImage(o);
					if (dataValue instanceof Image) return (Image) dataValue;
					if (dataValue instanceof ImageData) {
						// To prevent the image that is returned here from not being disposed we abuse the KeyCache.
						Image img = new Image(null, (ImageData) dataValue);
						markForDispose(img);
						return img;
					}
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
						Object o = getImageObject(columnAccessor, rowDataProvider, cell);
						if (!(o instanceof Compound)) text = super.getText(o);
						else text = null;
					} else {
						Compound rowObject = rowDataProvider.getRowObject(cell.getRowIndex());
						text = columnAccessor.getTooltipText(rowObject, cell.getColumnIndex());
					}
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
					Object o = getImageObject(columnAccessor, rowDataProvider, cell);
					super.fillAdvancedControls(parent, o, update);
				}
			}

			private Object getImageObject(CompoundImageContentProvider columnAccessor, IRowDataProvider<Compound> rowDataProvider, ILayerCell cell) {
				Compound compound = rowDataProvider.getRowObject(cell.getRowIndex());
				return columnAccessor.getSelectionValue(compound, cell.getColumnIndex());
			}
		});
	}

}
