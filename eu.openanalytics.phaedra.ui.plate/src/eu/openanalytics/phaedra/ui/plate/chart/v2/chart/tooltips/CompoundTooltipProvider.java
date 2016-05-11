package eu.openanalytics.phaedra.ui.plate.chart.v2.chart.tooltips;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.IOException;

import chemaxon.formats.MolFormatException;
import chemaxon.formats.MolImporter;
import chemaxon.marvin.util.DispOptConstants;
import chemaxon.struc.Molecule;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.TooltipsSettings;
import eu.openanalytics.phaedra.base.ui.charting.v2.data.IDataProvider;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.model.plate.compound.CompoundInfoService;
import eu.openanalytics.phaedra.model.plate.vo.Compound;
import uk.ac.starlink.ttools.plot.Tooltip;
import uk.ac.starlink.ttools.plot.TooltipsPlot.ITooltipProvider;

public class CompoundTooltipProvider<ENTITY, ITEM> implements ITooltipProvider {

	public static final String CFG_SIZE = "size";
	public static final String CFG_SHOW_STRUCTURE = "showStructure";

	public static final int DEFAULT_SIZE = 100;
	public static final boolean DEFAULT_SHOW_STRUCTURE = true;

	private IDataProvider<ENTITY, ITEM> dataProvider;
	private int size = DEFAULT_SIZE;
	private boolean showStructure = DEFAULT_SHOW_STRUCTURE;

	public CompoundTooltipProvider(IDataProvider<ENTITY, ITEM> dataProvider) {
		this.dataProvider = dataProvider;
	}

	@Override
	public void starting() {
		// Do nothing.
	}

	@Override
	public Point getTooltipSize(int index) {
		if (showStructure) {
			return new Point(size, size);
		} else {
			return new Point(0, 0);
		}
	}

	@Override
	public Tooltip getTooltip(int index) throws IOException {
		Compound c = SelectionUtils.getAsClass(dataProvider.getRowObject(index), Compound.class);
		// Check if this Well has a compound.
		if (c != null) {
			// Check if the structure should be shown.
			if (showStructure) {
				try {
					String smiles = CompoundInfoService.getInstance().getInfo(c).getSmiles();
					Molecule mol = MolImporter.importMol(smiles);
					if (mol != null) {
						BufferedImage im = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
						Graphics2D g = im.createGraphics();
						g.setColor(Color.white);
						g.fillRect(0, 0, im.getWidth(), im.getHeight());
						mol.draw(g, "w" + size + ",h" + size + DispOptConstants.RENDERING_STYLES[DispOptConstants.STICKS]);
						return new Tooltip(im, c.getType() + ", " + c.getNumber());
					}
				} catch (MolFormatException e) {}
			} else {
				return new Tooltip(null, c.getType() + ", " + c.getNumber());
			}
		}

		return null;
	}

	@Override
	public void setConfig(Object config) {
		if (config instanceof TooltipsSettings) {
			Object o = ((TooltipsSettings) config).getMiscSetting(CFG_SIZE);
			if (o instanceof Integer) {
				size = (int) o;
			}
			o = ((TooltipsSettings) config).getMiscSetting(CFG_SHOW_STRUCTURE);
			if (o instanceof Boolean) {
				showStructure = (boolean) o;
			}
		}
	}

	@Override
	public void dispose() {
		// Do nothing.
	}

}