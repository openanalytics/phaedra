package eu.openanalytics.phaedra.ui.curve.grid.tooltip;

import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.data.IRowDataProvider;
import org.eclipse.nebula.widgets.nattable.layer.LabelStack;
import org.eclipse.nebula.widgets.nattable.layer.cell.ILayerCell;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Resource;
import org.eclipse.swt.widgets.Composite;

import eu.openanalytics.phaedra.base.ui.nattable.misc.AdvancedNatTableToolTip;
import eu.openanalytics.phaedra.base.ui.util.tooltip.IToolTipUpdate;
import eu.openanalytics.phaedra.base.ui.util.tooltip.ToolTipLabelProvider;
import eu.openanalytics.phaedra.model.plate.vo.Compound;
import eu.openanalytics.phaedra.ui.curve.grid.provider.CompoundContentProvider;

public class CompoundToolTip extends AdvancedNatTableToolTip {

	private Resource disposeResource;

	public CompoundToolTip(NatTable table, CompoundContentProvider columnAccessor, IRowDataProvider<Compound> rowDataProvider) {
		super(table);

		setLabelProvider(new ToolTipLabelProvider() {
			@Override
			public Image getImage(Object element) {
				ILayerCell cell = (ILayerCell) element;
				if (isImageObject(cell.getDataValue())) {
					Compound compound = rowDataProvider.getRowObject(cell.getRowIndex());
					int[] size = { columnAccessor.getImageWidth(), columnAccessor.getImageHeight() };
					columnAccessor.setImageSize(size[0] * 3, size[1] * 3);
					ImageData imageData = (ImageData) columnAccessor.getDataValue(compound, cell.getColumnIndex());
					columnAccessor.setImageSize(size[0], size[1]);
					
					if (imageData != null) {
						Image img = new Image(null, (ImageData) imageData);
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
					if (!isImageObject(dataValue)) {
						Compound rowObject = rowDataProvider.getRowObject(cell.getRowIndex());
						text = columnAccessor.getTooltipText(rowObject, cell.getColumnIndex());
					} else {
						text = "";
					}
				} else if (isColumnCell(cellConfigLabels)) {
					text = columnAccessor.getTooltipText(null, cell.getColumnIndex());
				}

				if (text  == null) {
					text = getDefaultTextTooltip(cell);
				}

				return text;
			}

			@Override
			public void fillAdvancedControls(Composite parent, Object element, IToolTipUpdate update) {
				ILayerCell cell = (ILayerCell) element;
				Object dataValue = cell.getDataValue();
				if (isImageObject(dataValue)) {
					Object o = getImageObject(columnAccessor, rowDataProvider, cell);
					super.fillAdvancedControls(parent, o, update);
				}
			}

			private Object getImageObject(CompoundContentProvider columnAccessor, IRowDataProvider<Compound> rowDataProvider, ILayerCell cell) {
				Compound compound = rowDataProvider.getRowObject(cell.getRowIndex());
				return columnAccessor.getSelectionValue(compound, cell.getColumnIndex());
			}
		});
	}

	@Override
	public void dispose() {
		super.dispose();
		markForDispose(null);
	}

	/**
	 * Dispose the previous Resource and keep a reference to the current one so it can be disposed when needed.
	 *
	 * @param resource <code>null</code> to just dispose the previous resource
	 */
	private void markForDispose(Resource resource) {
		if (disposeResource != null) disposeResource.dispose();
		this.disposeResource = resource;
	}

}
