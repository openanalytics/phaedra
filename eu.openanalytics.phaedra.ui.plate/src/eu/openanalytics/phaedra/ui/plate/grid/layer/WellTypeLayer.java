package eu.openanalytics.phaedra.ui.plate.grid.layer;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;

import eu.openanalytics.phaedra.base.ui.gridviewer.widget.GridCell;
import eu.openanalytics.phaedra.base.ui.gridviewer.widget.render.BaseGridCellRenderer;
import eu.openanalytics.phaedra.base.ui.gridviewer.widget.render.IGridCellRenderer;
import eu.openanalytics.phaedra.base.util.misc.ColorStore;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;
import eu.openanalytics.phaedra.ui.plate.grid.PlatesLayer;

public class WellTypeLayer extends PlatesLayer {

	private ColorStore colorStore;

	public WellTypeLayer() {
		this.colorStore = new ColorStore();
	}

	@Override
	public String getName() {
		return "Well Type";
	}

	@Override
	protected void doInitialize() {
		if (colorStore == null) colorStore = new ColorStore();
	}

	@Override
	public IGridCellRenderer createRenderer() {
		return new WellTypeRenderer();
	}

	@Override
	public void dispose() {
		colorStore.dispose();
		colorStore = null;
		super.dispose();
	}

	private class WellTypeRenderer extends BaseGridCellRenderer {

		@Override
		public void render(GridCell cell, GC gc, int x, int y, int w, int h) {
			if (!isEnabled() || !hasPlates()) return;

			List<Well> wells = getWells(cell.getData());
			Set<String> wellTypes = new TreeSet<>();

			for (Well well : wells) {
				wellTypes.add(well.getWellType());
			}

			int size = wellTypes.size();
			int x1, x2, x3;
			int y2, y3, y4;
			x1 = x2 = x3 = x;
			y2 = y3 = y4 = y;
			int y1 = y + h;
			int x4 = x + w;

			int index = 0;
			for (String wellType : wellTypes) {
				// Calculate points
				if (index != 0) {
					int xValue = (int) ((w * (1d / size * 2)) * index);
					int yValue = (int) ((h * (1d / size * 2)) * index);
					if (index <= (size / 2)) {
						y2 = yValue + y;
						x3 = xValue + x;
					} else {
						x1 = Math.abs(w - xValue) + x;
						y4 = Math.abs(h - yValue) + y;
						x2 = x1;
						y2 = y1;
						x3 = x4;
						y3 = y4;
					}
				}

				// Draw
				Color color = colorStore.get(ProtocolUtils.getWellTypeRGB(wellType));
				gc.setBackground(color);
				gc.fillPolygon(new int[] {
						x1, y1 // First point			 ___3___4
						, x2, y2 // Second point		 |/		|
						, x3, y3 // Third point			2|		|
						, x4, y4 // Fourth point		 |		|
						, x+w, y+h // Final point		1|______|Final
				});
				index++;
			}
		}
	}

}