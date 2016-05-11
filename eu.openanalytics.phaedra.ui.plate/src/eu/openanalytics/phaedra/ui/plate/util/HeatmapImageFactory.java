package eu.openanalytics.phaedra.ui.plate.util;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Transform;

import eu.openanalytics.phaedra.base.ui.colormethod.IColorMethod;
import eu.openanalytics.phaedra.base.ui.colormethod.IColorMethodData;
import eu.openanalytics.phaedra.base.util.misc.ColorStore;
import eu.openanalytics.phaedra.base.util.misc.NumberUtils;
import eu.openanalytics.phaedra.calculation.CalculationService;
import eu.openanalytics.phaedra.calculation.PlateDataAccessor;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.ui.protocol.util.ColorMethodFactory;
import eu.openanalytics.phaedra.validation.ValidationService.WellStatus;

/**
 * Utility class to draw static heatmaps with status symbols on top.
 * Borrows code from HeatmapLayer, StatusLayer, Grid and GridHeaderCell.
 */
public class HeatmapImageFactory {

	public static ImageData drawHeatmap(HeatmapConfig cfg) {
		if (cfg.width == 0 || cfg.height == 0) return null;
		Image image = new Image(null, cfg.width, cfg.height);
		ImageData imageData = null;
		GC gc = new GC(image);
		try {
			drawHeatmap(gc, cfg);
			imageData = image.getImageData();
		} finally {
			gc.dispose();
			image.dispose();
		}
		return imageData;
	}
	
	public static void drawHeatmap(GC gc, HeatmapConfig cfg) {
		int rows = cfg.plate.getRows();
		int cols = cfg.plate.getColumns();
		
		Rectangle area = gc.getClipping();
		int headerSize = cfg.includeHeaders ? 15 : 0;
		int hSpacing = cfg.roundWells ? 1 : 2;
		int vSpacing = hSpacing;
		
		int cellWidth = ((area.width-headerSize) / cols) - (hSpacing);
		int cellHeight = cellWidth; //((area.height-headerSize) / rows) - (vSpacing);
		
		Transform t = null;
		if (cfg.centerHeatmap) {
			int widthRemaining = area.width - headerSize - (cellWidth+hSpacing)*cols;
			int heightRemaining = area.height - headerSize - (cellHeight+vSpacing)*rows;
			t = new Transform(gc.getDevice());
			t.translate(widthRemaining/2, heightRemaining/2);
			gc.setTransform(t);
		}
		
		ColorStore colorStore = new ColorStore();
		int oldAntiAliasState = gc.getAntialias();
		gc.setAntialias(SWT.ON);
		
		if (cfg.background != null) {
			gc.setBackground(colorStore.get(cfg.background));
			gc.fillRectangle(area);
		}
		
		if (cfg.includeHeaders) {
			gc.setBackground(colorStore.get(new RGB(99, 99, 99)));
			for (int row=0; row<rows; row++) {
				int y = headerSize + row*(cellHeight+vSpacing);
				gc.fillRectangle(0, y, headerSize, cellHeight+1);
				gc.setForeground(colorStore.get(new RGB(255, 255, 255)));
				gc.drawText(NumberUtils.getWellRowLabel(row+1), 2, y+2, true);
			}
			for (int col=0; col<cols; col++) {
				int x = headerSize + col*(cellWidth+hSpacing);
				gc.fillRectangle(x, 0, cellWidth+1, headerSize);
				gc.setForeground(colorStore.get(new RGB(255, 255, 255)));
				gc.drawText(""+(col+1), x+2, 2, true);
			}
		}
		
		IColorMethod colorMethod = null;
		if (cfg.feature != null) {
			colorMethod = ColorMethodFactory.createColorMethod(cfg.feature);
			PlateDataAccessor dataAccessor = CalculationService.getInstance().getAccessor(cfg.plate);
			IColorMethodData colorMethodData = cfg.experimentLimit ?
					ColorMethodFactory.createData(cfg.plate.getExperiment(), cfg.feature, cfg.normalization):
					ColorMethodFactory.createData(dataAccessor, cfg.feature, cfg.normalization);
			colorMethod.initialize(colorMethodData);
		}
		
		for (int row=0; row<rows; row++) {
			for (int col=0; col<cols; col++) {
				int x = headerSize+(col*(cellWidth+hSpacing));
				int y = headerSize+(row*(cellHeight+vSpacing));
				
				if (cfg.welltypeMap) drawWelltypeColor(gc, x, y, row, col, cellWidth, cellHeight, cfg, colorStore);
				else drawHeatmapColor(gc, x, y, row, col, cellWidth, cellHeight, cfg, colorStore, colorMethod);
				
				if (cfg.includeStatusSymbols) drawStatus(gc, x, y, row, col, cellWidth, cellHeight, cfg, colorStore);
			}
		}
		
		if (oldAntiAliasState != SWT.ON) gc.setAntialias(oldAntiAliasState);
		if (t != null) t.dispose();
		colorStore.dispose();
	}
	
	private static void drawWelltypeColor(GC gc, int x, int y, int row, int col, int cellWidth, int cellHeight, HeatmapConfig cfg, ColorStore colorStore) {
		Well well = PlateUtils.getWell(cfg.plate, row+1, col+1);
		RGB color = ProtocolUtils.getWellTypeRGB(well.getWellType());
		drawColor(gc, color, x, y, cellWidth, cellHeight, cfg, colorStore);
	}
	
	private static void drawHeatmapColor(GC gc, int x, int y, int row, int col, int cellWidth, int cellHeight, HeatmapConfig cfg, ColorStore colorStore, IColorMethod colorMethod) {
		PlateDataAccessor dataAccessor = CalculationService.getInstance().getAccessor(cfg.plate);
		RGB color = new RGB(150,150,150);
		if (cfg.feature.isNumeric()) {
			double value = dataAccessor.getNumericValue(row+1, col+1, cfg.feature, cfg.normalization);
			RGB lookupColor = colorMethod.getColor(value);
			if (!Double.isNaN(value) && lookupColor != null) color = lookupColor;
		} else {
			String value = dataAccessor.getStringValue(row+1, col+1, cfg.feature);
			RGB lookupColor = colorMethod.getColor(value);
			if (lookupColor != null) color = lookupColor;
		}
		drawColor(gc, color, x, y, cellWidth, cellHeight, cfg, colorStore);
	}
	
	private static void drawColor(GC gc, RGB color, int x, int y, int w, int h, HeatmapConfig cfg, ColorStore colorStore) {
		Color c = colorStore.get(color);
		gc.setBackground(c);
		if (cfg.roundWells) gc.fillOval(x, y, w+1, h+1);
		else gc.fillRectangle(x, y, w, h);

		if (w > 3 && h > 3) {
			gc.setLineWidth(1);
			gc.setForeground(colorStore.get(new RGB(128, 128, 128)));
			if (cfg.roundWells) gc.drawOval(x, y, w, h);
			else gc.drawRectangle(x, y, w, h);
		}
	}
	
	private static void drawStatus(GC gc, int x, int y, int row, int col, int cellWidth, int cellHeight, HeatmapConfig cfg, ColorStore colorStore) {
		Well well = PlateUtils.getWell(cfg.plate, row+1, col+1);
		
		// Draw cross on rejected wells.
		int status = well.getStatus();
		if (status < 0) {
			Color c = colorStore.get(new RGB(128, 128, 128));
			int lineWidth = (cellWidth > 10) ? 3 : 2;

			gc.setForeground(c);
			gc.setLineWidth(lineWidth+2);
			gc.drawLine(x, y, x+cellWidth, y+cellHeight);
			gc.drawLine(x, y+cellHeight, x+cellWidth, y);

			if (status == WellStatus.REJECTED_PHAEDRA.getCode()) {
				c = colorStore.get(new RGB(255, 0, 0));
			} else {
				c = colorStore.get(new RGB(255, 255, 0));
			}

			gc.setForeground(c);
			gc.setLineWidth(lineWidth);
			gc.drawLine(x, y, x+cellWidth, y+cellHeight);
			gc.drawLine(x, y+cellHeight, x+cellWidth, y);
		}
	}

	public static class HeatmapConfig {
		
		public Plate plate;
		public Feature feature;
		public String normalization;
		public boolean experimentLimit;
		
		public int width;
		public int height;
		
		public RGB background;
		
		public boolean centerHeatmap;
		public boolean roundWells;
		public boolean includeHeaders;
		public boolean includeStatusSymbols;
		public boolean welltypeMap;
		
		public HeatmapConfig() {
			// Default constructor
			this.experimentLimit = false;
			this.centerHeatmap = true;
			this.roundWells = true;
			this.includeHeaders = false;
			this.includeStatusSymbols = true;
		}

		public HeatmapConfig(Plate plate, Feature feature, int width, int height) {
			this();
			this.plate = plate;
			this.feature = feature;
			this.width = width;
			this.height = height;
			if (feature != null) this.normalization = feature.getNormalization();
		}
	}
}
