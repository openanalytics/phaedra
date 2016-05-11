package eu.openanalytics.phaedra.base.ui.colormethod.manminmax;

import java.util.Map;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Shell;

import eu.openanalytics.phaedra.base.ui.colormethod.BaseColorMethod;
import eu.openanalytics.phaedra.base.ui.colormethod.BaseColorMethodDialog;
import eu.openanalytics.phaedra.base.ui.colormethod.IColorMethodData;
import eu.openanalytics.phaedra.base.ui.colormethod.LegendDrawer;
import eu.openanalytics.phaedra.base.util.misc.ColorUtils;
import eu.openanalytics.phaedra.base.util.misc.NumberUtils;

public class ManualMinMaxColorMethod extends BaseColorMethod {

	private static final long serialVersionUID = 5537328712685758877L;
	
	public final static String SETTING_MIN_RGB = "min.rgb";
	public final static String SETTING_MAX_RGB = "max.rgb";
	public final static String SETTING_MIN_TYPE = "min.type";
	public final static String SETTING_MAX_TYPE = "max.type";
	public final static String SETTING_MIN_VAL = "min.val";
	public final static String SETTING_MAX_VAL = "max.val";
	
	public final static int TYPE_ABSOLUTE = 1;
	public final static int TYPE_PERCENTILE = 2;
	
	public final static RGB DEFAULT_MIN_COLOR = new RGB(255,255,255);
	public final static RGB DEFAULT_MAX_COLOR = new RGB(0,180,0);
	
	public final static int DEFAULT_TYPE = TYPE_PERCENTILE;
	
	public final static double DEFAULT_MIN_VAL = 5.0;
	public final static double DEFAULT_MAX_VAL = 95.0;
	
	private double min, max;
	private int minType, maxType;
	
	private RGB minRGB, maxRGB;
	private RGB[] gradients;

	private IColorMethodData currentData;
	
	@Override
	public void configure(Map<String, String> settings) {
		minRGB = getSetting(SETTING_MIN_RGB, settings, DEFAULT_MIN_COLOR);
		maxRGB = getSetting(SETTING_MAX_RGB, settings, DEFAULT_MAX_COLOR);
		minType = getSetting(SETTING_MIN_TYPE, settings, DEFAULT_TYPE);
		maxType = getSetting(SETTING_MAX_TYPE, settings, DEFAULT_TYPE);
		min = getSetting(SETTING_MIN_VAL, settings, DEFAULT_MIN_VAL);
		max = getSetting(SETTING_MAX_VAL, settings, DEFAULT_MAX_VAL);
		
		gradients = ColorUtils.createGradient(minRGB, maxRGB, 256);
	}
	
	@Override
	public void getConfiguration(Map<String, String> settings) {
		settings.put(SETTING_MIN_RGB, ColorUtils.createRGBString(minRGB));
		settings.put(SETTING_MAX_RGB, ColorUtils.createRGBString(maxRGB));
		settings.put(SETTING_MIN_TYPE, "" + minType);
		settings.put(SETTING_MAX_TYPE, "" + maxType);
		settings.put(SETTING_MIN_VAL, "" + min);
		settings.put(SETTING_MAX_VAL, "" + max);
	}
	
	@Override
	public void initialize(IColorMethodData dataset) {
		currentData = dataset;
	}
	
	@Override
	public RGB getColor(double v) {
		int index = getIndex(v);
		if (index == -1) return null;
		return gradients[index];
	}
	
	@Override
	public Image getLegend(int width, int height, int orientation, boolean labels, double[] highlightValues) {
		return getLegend(width, height, orientation, labels, highlightValues, false);
	}
	
	@Override
	public Image getLegend(int width, int height, int orientation, boolean labels, double[] highlightValues, boolean isWhiteBackground) {
		String[] valueLabels = null;
		String[] highlightLabels = null;
		
		if (minType == TYPE_PERCENTILE && maxType == TYPE_PERCENTILE) {
			// If the type is percentile, translate the values to make the legend scale correctly.
			valueLabels = new String[]{NumberUtils.round(getMinValue(), 2), NumberUtils.round(getMaxValue(), 2)};
			
			if (highlightValues != null) {
				highlightLabels = new String[highlightValues.length];
				double[] percentileHighlights = new double[highlightValues.length];
				for (int i=0; i<highlightValues.length; i++) {
					double ix = (double)getIndex(highlightValues[i]);
					percentileHighlights[i] = min + (int)(ix * ((max-min)/(gradients.length-1)));
					highlightLabels[i] = "" + highlightValues[i];
				}
				highlightValues = percentileHighlights;
			}
		}
		
		return new LegendDrawer(orientation).getLegend(
				new RGB[] {minRGB, maxRGB},
				new String[] {"Min", "Max"},
				new double[] {min, max},
				valueLabels,
				highlightValues,
				highlightLabels,
				width, height, isWhiteBackground);
	}
	
	@Override
	public BaseColorMethodDialog createDialog(Shell shell) {
		return new ManualMinMaxColorMethodDialog(shell, this);
	}
	
	private int getIndex(double v) {
		double minValue = getMinValue();
		double maxValue = getMaxValue();
		
		double diff = maxValue - minValue;
		if (diff == 0 || Double.isNaN(v)) return -1;

		double ratio = (gradients.length-1) / diff;

		if (v < minValue) return 0;
		if (minValue <= v && v <= maxValue) return (int)((v - minValue) * ratio);
		if (v > maxValue) return gradients.length-1;
		
		return -1;
	}

	private double getMinValue() {
		if (minType == TYPE_PERCENTILE) {
			if (currentData == null) return Double.NaN;
			if (min == 0) return currentData.getMin();
			else return currentData.getValue("pct" + (int)min);
		} else {
			return min;
		}
	}
	
	private double getMaxValue() {
		if (maxType == TYPE_PERCENTILE) {
			if (currentData == null) return Double.NaN;
			return currentData.getValue("pct" + (int)max);
		} else {
			return max;
		}
	}
}