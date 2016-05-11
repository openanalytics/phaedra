package eu.openanalytics.phaedra.ui.curve.grid.tooltip;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;

import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.data.IRowDataProvider;
import org.eclipse.nebula.widgets.nattable.layer.LabelStack;
import org.eclipse.nebula.widgets.nattable.layer.cell.ILayerCell;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Resource;
import org.eclipse.swt.widgets.Composite;

import chemaxon.formats.MolFormatException;
import chemaxon.formats.MolImporter;
import chemaxon.marvin.util.DispOptConstants;
import chemaxon.struc.Molecule;
import eu.openanalytics.phaedra.base.ui.nattable.misc.AdvancedNatTableToolTip;
import eu.openanalytics.phaedra.base.ui.util.tooltip.IToolTipUpdate;
import eu.openanalytics.phaedra.base.ui.util.tooltip.ToolTipLabelProvider;
import eu.openanalytics.phaedra.base.util.convert.AWTImageConverter;
import eu.openanalytics.phaedra.base.util.convert.PDFToImageConverter;
import eu.openanalytics.phaedra.model.curve.vo.Curve;
import eu.openanalytics.phaedra.model.plate.compound.CompoundInfoService;
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
				Object dataValue = cell.getDataValue();

				if (isImageObject(dataValue)) {
					Object o = getImageObject(columnAccessor, rowDataProvider, cell);
					if (o instanceof Compound && cell.getColumnIndex() == 7) return makeSmilesImage((Compound) o, 300, 300);
					if (o instanceof Curve) return convertCurve((Curve) o, 300, 300);
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

	private Image makeSmilesImage(Compound c, int width, int height) {
		String smiles = CompoundInfoService.getInstance().getInfo(c).getSmiles();

		Image img = null;
		try {
			Molecule mol = MolImporter.importMol(smiles);
			if (mol != null) {
				BufferedImage im = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
				Graphics2D g = im.createGraphics();
				g.setColor(Color.white);
				g.fillRect(0, 0, im.getWidth(), im.getHeight());
				mol.draw(g, "w" + width + ",h" + height + DispOptConstants.RENDERING_STYLES[DispOptConstants.STICKS]);
				img = AWTImageConverter.convert(null, im);
				// To prevent the image that is returned here from not being disposed we abuse the KeyCache.
				markForDispose(img);
			}
		} catch (MolFormatException e) {}
		return img;
	}

	private Image convertCurve(Curve curve, int width, int height) {
		Image img = null;
		if (curve != null && curve.getPlot() != null) {
			try {
				img = PDFToImageConverter.convert(curve.getPlot(), width, height);
				// To prevent the image that is returned here from not being disposed we abuse the KeyCache.
				markForDispose(img);
			} catch (IOException e) {}
		}
		return img;
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
