package eu.openanalytics.phaedra.ui.plate.grid.layer;

import java.util.List;

import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.RGB;

import eu.openanalytics.phaedra.base.ui.gridviewer.widget.GridCell;
import eu.openanalytics.phaedra.base.ui.gridviewer.widget.render.BaseGridCellRenderer;
import eu.openanalytics.phaedra.base.ui.gridviewer.widget.render.IGridCellRenderer;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.ui.plate.Activator;
import eu.openanalytics.phaedra.ui.plate.grid.PlatesLayer;
import eu.openanalytics.phaedra.ui.plate.preferences.Prefs;
import eu.openanalytics.phaedra.validation.ValidationService.WellStatus;

public class StatusLayer extends PlatesLayer {

	@Override
	public String getName() {
		return "Status Indicators";
	}

	@Override
	protected void doInitialize() {
		// Nothing to do.
	}

	@Override
	public IGridCellRenderer createRenderer() {
		return new WellGridStatusRenderer();
	}

	private class WellGridStatusRenderer extends BaseGridCellRenderer {

		@Override
		public void render(GridCell cell, GC gc, int x, int y, int w, int h) {
			if (!isEnabled() || !hasPlates()) return;

			List<Well> wells = getWells(cell.getData());
			if (wells == null || wells.isEmpty()) return;

			int oldAntiAliasState = gc.getAntialias();
			gc.setAntialias(SWT.ON);

			int lineWidth = 3;
			int type = 1;
			for (int i = 10; i > 1; i--) {
				if (wells.size() != i && wells.size() % i == 0) {
					type = i;
					lineWidth = 2;
					break;
				}
			}

			int typeW = w / type;
			int typeH = h / (wells.size() / type);
			int col = -1;
			int row = 0;
			for (Well well : wells) {
				if (well != null) {
					col++;
					if (col == type) {
						col = 0;
						row += 1;
					}

					// Draw cross on rejected wells.
					int status = well.getStatus();
					if (status < 0) {
						int x1 = x + (typeW * col);
						int x2 = x + (typeW * (col+1));
						int y1 = y + (typeH * row);
						int y2 = y + (typeH * (row+1));

						Color c = gc.getDevice().getSystemColor(SWT.COLOR_GRAY);

						gc.setForeground(c);
						gc.setLineWidth(lineWidth+2);
						gc.drawLine(x1,y1,x2,y2);
						gc.drawLine(x2,y1,x1,y2);

						if (status == WellStatus.REJECTED_PHAEDRA.getCode()) {
							c = gc.getDevice().getSystemColor(SWT.COLOR_RED);
						} else if (status == WellStatus.REJECTED_DATACAPTURE.getCode()) {
							c = gc.getDevice().getSystemColor(SWT.COLOR_BLUE);
						} else {
							c = gc.getDevice().getSystemColor(SWT.COLOR_YELLOW);
						}

						gc.setForeground(c);
						gc.setLineWidth(lineWidth);
						gc.drawLine(x1,y1,x2,y2);
						gc.drawLine(x2,y1,x1,y2);
					}
				}
			}

			// Draw triangles on wells with well- or subwell-annotations.
			if (Activator.getDefault().getPreferenceStore().getBoolean(Prefs.HEATMAP_ANNOTATIONS)) {
				boolean annotationWell = false;
				boolean annotationCell = false;
				for (Well well : wells) {
					if (well.getDescription() != null) {
						annotationWell = true;
					}
				}
				if (annotationWell || annotationCell) {
					RGB wellRGB = new RGB(0,0,0);
					RGB subwellRGB = new RGB(0,0,0);
					if (annotationWell) wellRGB = PreferenceConverter.getColor(Activator.getDefault().getPreferenceStore(), Prefs.HEATMAP_ANNOTATION_WELL_COLOR);
					if (annotationCell) subwellRGB = PreferenceConverter.getColor(Activator.getDefault().getPreferenceStore(), Prefs.HEATMAP_ANNOTATION_SUBWELL_COLOR);
					int r = wellRGB.red | subwellRGB.red;
					int g = wellRGB.green | subwellRGB.green;
					int b = wellRGB.blue | subwellRGB.blue;
					Color colorSetting = new Color(null, r, g, b);

					gc.setBackground(colorSetting);
					gc.setLineWidth(2);
					int size = (int) ((w - 2) / (100f / Activator.getDefault().getPreferenceStore().getInt(Prefs.HEATMAP_ANNOTATION_SIZE)));
					int[] points = new int[] { x, y, x + size, y, x, y + size };
					gc.fillPolygon(points);
					colorSetting.dispose();
				}
			}

			if (oldAntiAliasState != SWT.ON) {
				gc.setAntialias(oldAntiAliasState);
			}
		}

	}
}
