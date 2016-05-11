package eu.openanalytics.phaedra.base.ui.charting.v2.grouping;

import java.awt.Color;

import eu.openanalytics.phaedra.base.ui.charting.preferences.Prefs;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.ChartSettings;
import uk.ac.starlink.ttools.plot.BarStyle;
import uk.ac.starlink.ttools.plot.BarStyle.Form;
import uk.ac.starlink.ttools.plot.MarkShape;
import uk.ac.starlink.ttools.plot.MarkStyle;
import uk.ac.starlink.ttools.plot.Style;

public class DefaultStyleProvider implements IStyleProvider {

	public static final String BARSTYLE_STEPS = "Steps";
	public static final String BARSTYLE_FILLED_3D = "Filled 3D";
	public static final String BARSTYLE_FILLED = "Filled";
	public static final String BARSTYLE_SPIKES = "Spikes";
	public static final String BARSTYLE_OPEN = "Open";

	public final static String OPEN_CIRCLE = "Open circles";
	public final static String FILLED_CIRCLE = "Filled circles";
	public final static String OPEN_RECTANGLE = "Open rectangles";
	public final static String FILLED_RECTANGLE = "Filled rectangles";

	private Color[] defaultColors = { Color.BLUE, Color.RED, Color.GREEN, Color.GRAY, Color.ORANGE, Color.YELLOW,
			Color.CYAN, new Color(0, 153, 153), new Color(0, 102, 51), new Color(102, 0, 0),
			new Color(102, 102, 255), new Color(153, 255, 255), new Color(204, 0, 102), new Color(204, 255, 102),
			new Color(240, 204, 0), new Color(153, 0, 255), new Color(0, 255, 255), new Color(153, 153, 255),
			new Color(255, 102, 0), new Color(204, 51, 204), Color.BLACK };

	@Override
	public synchronized Style[] getStyles(String[] groups, ChartSettings settings) {
		int groupCount = groups.length;
		Style[] updatedStyles = new Style[groupCount];

		boolean isNoGroup = groupCount == 1 && getColors() == defaultColors;

		for (int i = 0; i < groupCount; i++) {
			// Only one group that has no custom colors (e.g. Well Type Group), use default color.
			Color color = isNoGroup ? settings.getDefaultColor() : getColor(i);
			Style originalStyle = settings.getStyle(groups[i]);
			Style updatedStyle = getStyle(settings, color, originalStyle);
			settings.putStyle(groups[i], updatedStyle);
			updatedStyles[i] = updatedStyle;
		}
		return updatedStyles;
	}

	private Style getStyle(ChartSettings settings, Color color, Style originalStyle) {
		Style style;
		if (settings.isBars()) {
			style = getBarStyle(settings, color, originalStyle);
		} else {
			style = getMarkStyle(settings, color, originalStyle);
		}
		if (originalStyle != null) {
			style.setHidePoints(originalStyle.getHidePoints());
			style.setOpacity(originalStyle.getOpacity());
		} else {
			style.setHidePoints(false);
		}

		return style;
	}

	private MarkStyle getMarkStyle(ChartSettings settings, Color color, Style originalStyle) {
		MarkShape defaultShape = getMarkShapeByString(settings.getDefaultSymbolType());
		MarkStyle style = defaultShape.getStyle(color,
				originalStyle != null ? ((MarkStyle) originalStyle).getSize() : settings.getDefaultSymbolSize(), 0);
		if (settings.isLines()){
			style.setLine(MarkStyle.DOT_TO_DOT);
			style.setLineWidth(style.getSize());
		}
		return style;
	}

	private BarStyle getBarStyle(ChartSettings settings, Color color, Style originalStyle) {
		return new BarStyle(color, getBarFormByString(settings.getDefaultSymbolType()),
				settings.isAdjacentBars() ? BarStyle.PLACE_ADJACENT : BarStyle.PLACE_OVER);
	}

	private Color getColor(int position) {
		if (getColors().length != 0) {
			if (position < getColors().length) {
				return getColors()[position];
			} else {
				// more colors needed than defined, start all over
				return getColors()[Math.max(position % getColors().length, 0)];
			}
		}
		return Prefs.getDefaultColor();
		//return Color.BLUE;
	}

	public Color[] getColors() {
		return defaultColors;
	}

	public static MarkShape getMarkShapeByString(String style) {
		switch (style) {
		case FILLED_CIRCLE:
			return MarkShape.FILLED_CIRCLE;
		case OPEN_CIRCLE:
			return MarkShape.OPEN_CIRCLE;
		case FILLED_RECTANGLE:
			return MarkShape.FILLED_SQUARE;
		case OPEN_RECTANGLE:
			return MarkShape.OPEN_SQUARE;
		default:
			return MarkShape.FILLED_CIRCLE;
		}
	}

	public static Form getBarFormByString(String form) {
		switch (form) {
		case BARSTYLE_OPEN:
			return BarStyle.FORM_OPEN;
		case BARSTYLE_SPIKES:
			return BarStyle.FORM_SPIKE;
		case BARSTYLE_FILLED:
			return BarStyle.FORM_FILLED;
		case BARSTYLE_FILLED_3D:
			return BarStyle.FORM_FILLED3D;
		case BARSTYLE_STEPS:
			return BarStyle.FORM_TOP;
		default:
			return BarStyle.FORM_FILLED;
		}
	}

}