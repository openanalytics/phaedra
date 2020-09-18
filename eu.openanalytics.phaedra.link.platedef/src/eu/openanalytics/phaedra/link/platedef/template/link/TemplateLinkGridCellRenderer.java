package eu.openanalytics.phaedra.link.platedef.template.link;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.RGB;

import eu.openanalytics.phaedra.base.ui.gridviewer.widget.GridCell;
import eu.openanalytics.phaedra.base.ui.gridviewer.widget.render.BaseGridCellRenderer;
import eu.openanalytics.phaedra.base.util.misc.ColorStore;
import eu.openanalytics.phaedra.link.platedef.template.WellTemplate;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;
import eu.openanalytics.phaedra.model.protocol.vo.WellType;

public class TemplateLinkGridCellRenderer extends BaseGridCellRenderer {
	
	private ColorStore colorStore = new ColorStore();
	
	public String[] getLabels(GridCell cell) {
		WellTemplate template = (WellTemplate)cell.getData();
		if (template == null) return super.getLabels(cell);
		if (template.isSkip()) return new String[]{"SKIP"};
		String lbl1 = "";
		if (template.getCompoundType() != null && template.getCompoundNumber() == null) {
			lbl1 = template.getCompoundType() + " ???";
		}
		else if (template.getCompoundType() == null && template.getCompoundNumber() != null) {
			lbl1 = "??? " + template.getCompoundNumber();
		}
		else if (template.getCompoundType() != null && template.getCompoundNumber() != null) {
			lbl1 = template.getCompoundType() + " " + template.getCompoundNumber();
		}
		String lbl2 = "";
		if (template.getConcentration() != null) lbl2 = template.getConcentration();
		return new String[] {lbl1, lbl2};
	};
	
	@Override
	public Color getBgColor(GridCell cell) {
		WellTemplate template = (WellTemplate)cell.getData();
		if (template == null) return super.getBgColor(cell);
		if (template.isSkip()) return super.getBgColor(cell);
		RGB rgb = ProtocolUtils.getWellTypeRGB(template.getWellType());
		if (template.getWellType().equals(WellType.SAMPLE)) rgb = new RGB(80,80,200);
		if (rgb != null) return colorStore.get(rgb);
		return super.getBgColor(cell);
	}
	
	@Override
	public void render(GridCell cell, GC gc, int x, int y, int w, int h) {
		gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_BLACK));
		super.render(cell, gc, x, y, w, h);
	}
	
	@Override
	public void dispose() {
		colorStore.dispose();
		super.dispose();
	}
}
