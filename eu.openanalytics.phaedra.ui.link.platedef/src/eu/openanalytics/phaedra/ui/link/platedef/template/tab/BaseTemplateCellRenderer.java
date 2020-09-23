package eu.openanalytics.phaedra.ui.link.platedef.template.tab;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;

import eu.openanalytics.phaedra.base.ui.gridviewer.widget.GridCell;
import eu.openanalytics.phaedra.base.ui.gridviewer.widget.render.BaseGridCellRenderer;
import eu.openanalytics.phaedra.base.ui.util.misc.ColorCache;
import eu.openanalytics.phaedra.base.util.misc.ColorUtils;
import eu.openanalytics.phaedra.base.util.misc.NumberUtils;
import eu.openanalytics.phaedra.link.platedef.template.WellTemplate;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;

public abstract class BaseTemplateCellRenderer extends BaseGridCellRenderer {
	
	@Override
	public String[] getLabels(GridCell cell) {
		WellTemplate template = (WellTemplate)cell.getData();
		if (template == null) return super.getLabels(cell);
		if (template.isSkip()) return null;
		return doGetLabels(template);
	}
	
	@Override
	public Color getBgColor(GridCell cell) {
		WellTemplate template = (WellTemplate)cell.getData();
		if (template == null) return super.getBgColor(cell);
		if (template.isSkip()) return null;
		if (!ProtocolUtils.isControl(template.getWellType()) && NumberUtils.isNumeric(template.getConcentration())) {
			double conc = Double.parseDouble(template.getConcentration());
			return ColorCache.get(ProtocolUtils.getWellConcRGB(conc));
		} else {
			// PHA-644
			String wellType = ProtocolUtils.getCustomHCLCLabel(template.getWellType());
			return ColorCache.get(ProtocolUtils.getWellTypeRGB(wellType));
		}
	}
	
	@Override
	public void render(GridCell cell, GC gc, int x, int y, int w, int h) {
		WellTemplate template = (WellTemplate)cell.getData();
		if (template != null && template.isSkip()) {
			Color fg = gc.getForeground();
			gc.setLineWidth(1);
			gc.setForeground(ColorCache.get(0xAAAAAA));
			int ox = x+5;
			int oy = y+5;
			while (ox < 2*(x+w)) {
				gc.drawLine(x+1, oy, ox, y+1);
				ox += 15;
				oy += 15;
			}
			gc.setForeground(fg);
		}
		Color bgColor = getBgColor(cell);
		if (bgColor != null) gc.setForeground(ColorUtils.getTextColor(bgColor.getRGB()));
		super.render(cell, gc, x, y, w, h);
	}
	
	protected abstract String[] doGetLabels(WellTemplate well);
}
