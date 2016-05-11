package eu.openanalytics.phaedra.base.ui.colormethod;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

import eu.openanalytics.phaedra.base.ui.colormethod.lookup.LookupColorMethod;
import eu.openanalytics.phaedra.base.ui.colormethod.lookup.LookupRule;
import eu.openanalytics.phaedra.base.util.misc.ColorStore;

public class LegendDrawer {

	private int barMargin;
	private int labelMargin;
	private int valueMargin;

	private boolean horizontal;

	private final static int AREA_LABELS = 1;
	private final static int AREA_VALUES = 2;
	private final static int AREA_BARS = 3;
	private final static int AREA_LINES = 4;

	private final static DecimalFormat VALUE_FORMAT = new DecimalFormat("0.##");

	public LegendDrawer(int orientation) {
		barMargin = 5;
		labelMargin = 30;
		valueMargin = 40;
		horizontal = orientation == SWT.HORIZONTAL;
		if (horizontal) labelMargin = 15;
		if (horizontal) valueMargin = 15;
	}

	public Image getLegend(RGB[] rgbs, String[] labels, double[] values, double[] highlightValues, int width, int height, boolean isWhiteBackground) {
		return getLegend(rgbs, labels, values, null, highlightValues, null, width, height, isWhiteBackground);
	}

	public Image getLegend(RGB[] rgbs, String[] labels, double[] values, String[] valueLabels, double[] highlightValues, String[] highlightLabels,
			int width, int height, boolean isWhiteBackground) {

		Image image = new Image(Display.getDefault(), width, height);
		GC gc = new GC(image);
		Font legendFont = new Font(gc.getDevice(), "Tahoma", 7, SWT.NORMAL);

		try {
			gc.setFont(legendFont);

			boolean drawLabels = (labels != null && labels.length > 0);
			boolean drawValues = (values != null && values.length > 0);
			Rectangle totalArea = gc.getClipping();

			if (drawLabels) labelMargin = calculateLabelMargin(gc, labels);
			if (drawValues) valueMargin = calculateValueMargin(gc, values);

			setBackgroundColor(gc, isWhiteBackground);
			gc.fillRectangle(gc.getClipping());

			// Draw labels on left / top.
			if (drawLabels) {
				gc.setClipping(calculateArea(AREA_LABELS, totalArea, drawLabels, drawValues));
				drawLabels(gc, labels, horizontal);
			}

			// Draw colored bars.
			gc.setClipping(calculateArea(AREA_BARS, totalArea, drawLabels, drawValues));
			drawBars(gc, rgbs, horizontal);

			// Draw lines across bars.
			gc.setClipping(calculateArea(AREA_LINES, totalArea, drawLabels, drawValues));
			drawLines(gc, rgbs, horizontal);

			// Draw values.
			if (drawValues) {
				gc.setClipping(calculateArea(AREA_VALUES, totalArea, drawLabels, drawValues));
				drawValues(gc, values, valueLabels, horizontal);
			}

			// Draw highlighted points.
			if (highlightValues != null) {
				gc.setClipping(calculateArea(AREA_BARS, totalArea, drawLabels, drawValues));
				drawHighlightValues(gc, values, highlightValues, highlightLabels, horizontal, isWhiteBackground);
			}
		} finally {
			gc.dispose();
			legendFont.dispose();
		}

		return image;
	}

	public Image getLookupLegend(List<LookupRule> rules, double[] highlightValues, int width, int height, boolean isWhiteBackground) {
		return getLookupLegend(rules, null, highlightValues, width, height, isWhiteBackground);
	}
	
	public Image getLookupLegend(List<LookupRule> rules, String[] labels, double[] highlightValues, int width, int height, boolean isWhiteBackground) {

		Image image = new Image(Display.getDefault(), width, height);
		GC gc = new GC(image);
		ColorStore store = new ColorStore();
		Color bgColor = gc.getDevice().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);
		Rectangle totalArea = gc.getClipping();

		try {
			if (!isWhiteBackground) {
				// Fill entire background.
				gc.setBackground(bgColor);
			}
			gc.fillRectangle(gc.getClipping());

			// Draw labels on left / top.
			gc.setClipping(calculateArea(AREA_LABELS, totalArea, true, true));
			if (labels == null || labels.length != rules.size()) {
				labels = new String[rules.size()];
				for (int i=0; i<labels.length; i++) {
					labels[i] = LookupColorMethod.getConditionLabel(rules.get(i).getCondition()) + " " + rules.get(i).getValue();
				}
			}
			drawLabels(gc, labels, horizontal);

			// Draw colored bars.
			gc.setClipping(calculateArea(AREA_BARS, totalArea, true, true));
			if (rules.isEmpty()) {
				gc.setBackground(store.get(new RGB(100, 100, 100)));
				gc.fillRectangle(0, 0, width, height);
			} else {
				int pixelsPerRule = height / rules.size();
				if (horizontal) pixelsPerRule = width / rules.size();

				for (int i=0; i<rules.size(); i++) {
					LookupRule rule = rules.get(i);
					int x = 0;
					int y = (rules.size() - i - 1)*pixelsPerRule;
					int w = width;
					int h = pixelsPerRule;
					if (horizontal) {
						x = i*pixelsPerRule;
						y = 0;
						w = pixelsPerRule;
						h = height;
					}
					gc.setBackground(store.get(rule.getColor()));
					gc.fillRectangle(x,y,w,h);
				}
			}

			if (!rules.isEmpty()) {
				// Draw lines across bars.
				gc.setClipping(calculateArea(AREA_LINES, totalArea, true, true));
				drawLines(gc, new RGB[rules.size()+1], horizontal);
			}

			gc.drawRectangle(0, 0, width-1, height-1);
		} finally {
			store.dispose();
			gc.dispose();
		}
		return image;
	}

	private void drawBars(GC gc, RGB[] rgbs, boolean horizontal) {

		Color[] colors = new Color[rgbs.length];
		for (int i=0; i<colors.length; i++) {
			colors[i] = new Color(gc.getDevice(), rgbs[i]);
		}
		Color grey = new Color(gc.getDevice(), 100, 100, 100);

		int[] blockSize = getBlockSize(gc.getClipping(), rgbs.length-1, horizontal);
		Point clip = new Point(gc.getClipping().x, gc.getClipping().y);

		int offsetX = clip.x;
		int offsetY = clip.y;
		Color fg = null;
		Color bg = null;

		try {
			for (int i=0; i<colors.length-1; i++) {
				if (horizontal) {
					offsetX = clip.x + i*blockSize[0];
					fg = colors[i];
					bg = colors[i+1];
				} else {
					offsetY = clip.y + (colors.length-i-2)*blockSize[1];
					fg = colors[i+1];
					bg = colors[i];
				}

				gc.setForeground(fg);
				gc.setBackground(bg);
				gc.fillGradientRectangle(offsetX, offsetY, blockSize[0], blockSize[1], !horizontal);
			}

			gc.setForeground(grey);
			if (horizontal) {
				gc.drawLine(clip.x, clip.y, clip.x+(blockSize[0]*(rgbs.length-1))-1, clip.y);
				gc.drawLine(clip.x, clip.y+blockSize[1]-1, clip.x+(blockSize[0]*(rgbs.length-1)), clip.y+blockSize[1]-1);
			} else {
				gc.drawLine(clip.x, clip.y, clip.x, clip.y+(blockSize[1]*(rgbs.length-1))-1);
				gc.drawLine(clip.x+blockSize[0]-1, clip.y, clip.x+blockSize[0]-1, clip.y+(blockSize[1]*(rgbs.length-1))-1);
			}
		} finally {
			for (Color c: colors) c.dispose();
			grey.dispose();
		}
	}

	private void drawLines(GC gc, RGB[] rgbs, boolean horizontal) {

		int[] blockSize = getBlockSize(gc.getClipping(), rgbs.length-1, horizontal);
		Point clip = new Point(gc.getClipping().x, gc.getClipping().y);

		int x1 = clip.x;
		int y1 = clip.y;
		int x2 = clip.x;
		int y2 = clip.y;

		Color greyColor = new Color(gc.getDevice(), 100, 100, 100);
		try {
			gc.setForeground(greyColor);

			for (int i=0; i<rgbs.length; i++) {
				if (horizontal) {
					x1 = clip.x + i*blockSize[0];
					y2 = clip.y + rgbs.length*blockSize[1];
					if (i == rgbs.length-1) x1--;
					x2 = x1;
				} else {
					y1 = clip.y + i*blockSize[1];
					x2 = clip.x + blockSize[0];
					if (i == rgbs.length-1) y1--;
					y2 = y1;
				}

				gc.drawLine(x1, y1, x2, y2);
			}

		} finally {
			greyColor.dispose();
		}
	}

	private void drawLabels(GC gc, String[] labels, boolean horizontal) {

		int[] blockSize = getBlockSize(gc.getClipping(), labels.length-1, horizontal);

		int x = 0, y = 0;

		Color greyColor = new Color(gc.getDevice(), 100, 100, 100);
		try {
			gc.setForeground(greyColor);

			for (int i=0; i<labels.length; i++) {
				String lbl = labels[i];
				if (!horizontal) lbl = labels[labels.length - i - 1];
				Point extent = gc.textExtent(lbl);

				if (horizontal) {
					x = i*blockSize[0] - extent.x/2;
					if (i == 0) x = barMargin * 2;
					if (i == labels.length-1) x = i*blockSize[0] - extent.x - (barMargin * 2);
				} else {
					y = i*blockSize[1] - extent.y/2;
					if (i == 0) y += extent.y/2;
					if (i == labels.length-1) y -= extent.y/2;
				}

				gc.drawText(lbl, x, y);
			}

		} finally {
			greyColor.dispose();
		}
	}

	private void drawValues(GC gc, double[] values, String[] valueLabels, boolean horizontal) {

		if (valueLabels == null || valueLabels.length < values.length) {
			valueLabels = new String[values.length];
			for (int i=0; i<valueLabels.length; i++) {
				valueLabels[i] = VALUE_FORMAT.format(values[i]);
			}
		}

		int[] blockSize = getBlockSize(gc.getClipping(), values.length-1, horizontal);

		int x = 0, y = 0;

		Color greyColor = new Color(gc.getDevice(), 100, 100, 100);
		try {
			gc.setForeground(greyColor);

			for (int i=0; i<values.length; i++) {
				String lbl = valueLabels[i];
				if (!horizontal) lbl = valueLabels[values.length - i - 1];
				if (DecimalFormatSymbols.getInstance().getNaN().equals(lbl)) lbl = "0";
				Point extent = gc.textExtent(lbl);

				if (horizontal) {
					x = i*blockSize[0] - extent.x/2;
					y = gc.getClipping().y + barMargin;
					if (i == 0) x = barMargin * 2;
					if (i == values.length-1) x = i*blockSize[0] - extent.x - (barMargin * 2);
				} else {
					x = gc.getClipping().x + 2;
					y = i*blockSize[1] - extent.y/2;
					if (i == 0) y += extent.y/2;
					if (i == values.length-1) y -= extent.y/2;
				}

				gc.drawText(lbl, x, y, true);
			}

		} finally {
			greyColor.dispose();
		}
	}

	private void drawHighlightValues(GC gc, double[] values, double[] highlightValues, String[] highlightLabels, boolean horizontal, boolean isWhiteBackground) {

		if (highlightLabels == null || highlightLabels.length < highlightValues.length) {
			highlightLabels = new String[highlightValues.length];
			for (int i=0; i<highlightLabels.length; i++) {
				highlightLabels[i] = VALUE_FORMAT.format(highlightValues[i]);
			}
		}

		int x = 0, y = 0;
		int w = 10, h = w;

		int lineX1 = 0, lineX2 = 0, lineY1 = 0, lineY2 = 0;

		gc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_BLACK));
		Color greyColor = new Color(gc.getDevice(), 100, 100, 100);
		gc.setForeground(greyColor);
		try {
			for (int i=0; i<highlightValues.length; i++) {
				double v = highlightValues[i];
				if (Double.isNaN(v)) continue;

				// Calculate the position of the value as a percentage.
				int block = -1;
				for (int j=0; j<values.length; j++) {
					if (v > values[j]) block++;
				}

				int blocks = values.length-1;

				if (block < 0) block = 0;
				if (block >= blocks) block = blocks - 1;

				double interval = values[block+1] - values[block];
				int pct = (int)(100*(v - values[block])/interval);
				pct = pct / blocks;
				int pctPerBlock = 100/blocks;
				pct = (block*pctPerBlock) + pct;

				if (horizontal) {
					double range = gc.getClipping().width;
					double pixel = (range*pct) / 100;
					x = (int)pixel;
					y = gc.getClipping().y + gc.getClipping().height / 2;
					lineX1 = x;
					lineX2 = x;
					lineY1 = gc.getClipping().y;
					lineY2 = gc.getClipping().y + gc.getClipping().height;
				} else {
					double range = gc.getClipping().height;
					double pixel = (range*(100-pct)) / 100;
					x = gc.getClipping().x + gc.getClipping().width / 2;
					y = (int)pixel;
					lineX1 = gc.getClipping().x;
					lineX2 = gc.getClipping().x + gc.getClipping().width;
					lineY1 = y;
					lineY2 = y;
				}

				gc.drawLine(lineX1, lineY1, lineX2, lineY2);
				gc.fillOval(x-(w/2), y-(h/2), w+1, h+1);
				gc.drawOval(x-(w/2), y-(h/2), w, h);

				// Draw the value near the marker.
				gc.setClipping((Rectangle) null);
				Rectangle clip = calculateArea(AREA_VALUES, gc.getClipping(), false, true);
				gc.setClipping(clip);
				String lbl = highlightLabels[i];
				Point extent = gc.textExtent(lbl);
				setBackgroundColor(gc, isWhiteBackground);
				if (horizontal) {
					gc.drawText(lbl, x-extent.x/2, clip.y + barMargin);
				} else {
					gc.drawText(lbl, clip.x + 2, y-extent.y/2);
				}
			}
		} finally {
			greyColor.dispose();
		}
	}

	private void setBackgroundColor(GC gc, boolean isWhiteBackground) {
		gc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_WHITE));
	}

	private int[] getBlockSize(Rectangle area, int blockCount, boolean horizontal) {

		int blockWidth = 0;
		int blockHeight = 0;

		if (horizontal) {
			blockWidth = area.width / blockCount;
			blockHeight = area.height;
		} else {
			blockWidth = area.width;
			blockHeight = area.height / blockCount;
		}

		return new int[] {blockWidth, blockHeight};
	}

	private int calculateLabelMargin(GC gc, String[] labels) {
		int margin = 0;
		for (String label : labels) {
			Point textExtent = gc.textExtent(label);
			if (horizontal) {
				margin = Math.max(margin, textExtent.y + barMargin);
			} else {
				margin = Math.max(margin, textExtent.x + 1);
			}
		}
		return margin;
	}

	private int calculateValueMargin(GC gc, double[] values) {
		int margin = 0;
		for (double value : values) {
			Point textExtent = gc.textExtent(VALUE_FORMAT.format(value));
			if (horizontal) {
				margin = Math.max(margin, textExtent.y + barMargin);
			} else {
				margin = Math.max(margin, textExtent.x + 2);
			}
		}
		return margin;
	}

	private Rectangle calculateArea(int areaType, Rectangle totalArea, boolean drawLabels, boolean drawValues) {
		Rectangle area = new Rectangle(0,0,0,0);
		area.height = totalArea.height;
		switch (areaType) {
		case AREA_LABELS:
			// Left portion of the total area, regardless of other areas.
			area.width = totalArea.width;
			break;
		case AREA_LINES:
			if (horizontal) {
				area.x = barMargin;
				area.width = totalArea.width - (2*barMargin);
				if (drawLabels) {
					area.y += labelMargin - barMargin;
					area.height -= labelMargin - barMargin;
				}
				if (drawValues) {
					area.height -= valueMargin - barMargin;
				}
			} else {
				// Center portion of the total area, minus label margin left and value margin right.
				area.width = totalArea.width;
				if (drawLabels) {
					area.x = labelMargin;
					area.width -= labelMargin;
				}
				if (drawValues) {
					area.width -= valueMargin;
				}
			}
			break;
		case AREA_BARS:
			// Center portion of the total area, minus bar margin, label margin left and value margin right.
			area.x = barMargin;
			area.width = totalArea.width - (2*barMargin);
			if (horizontal) {
				if (drawLabels) {
					area.y += labelMargin;
					area.height -= labelMargin;
				}
				if (drawValues) {
					area.height -= valueMargin;
				}
			} else {
				if (drawLabels) {
					area.x += labelMargin;
					area.width -= labelMargin;
				}
				if (drawValues) {
					area.width -= valueMargin;
				}
			}
			break;
		case AREA_VALUES:
			if (horizontal) {
				area.y = totalArea.height - valueMargin;
				area.width = totalArea.width;
			} else {
				// Right portion of the total area
				area.x = totalArea.width - valueMargin;
				area.width = valueMargin;
			}
			break;
		}
		return area;
	}
}
