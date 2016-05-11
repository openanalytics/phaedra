package eu.openanalytics.phaedra.base.ui.colormethod.manminmeanmax;

import java.util.Map;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Shell;

import eu.openanalytics.phaedra.base.ui.colormethod.BaseColorMethod;
import eu.openanalytics.phaedra.base.ui.colormethod.BaseColorMethodDialog;
import eu.openanalytics.phaedra.base.ui.colormethod.IColorMethodData;
import eu.openanalytics.phaedra.base.ui.colormethod.LegendDrawer;
import eu.openanalytics.phaedra.base.util.misc.ColorUtils;

public class ManualMinMeanMaxColorMethod extends BaseColorMethod {

	private static final long serialVersionUID = -6241674415424642711L;
	
	public final static String SETTING_MIN_RGB = "min.rgb";
	public final static String SETTING_MEAN_RGB = "mean.rgb";
	public final static String SETTING_MAX_RGB = "max.rgb";
	public final static String SETTING_MIN_TYPE = "min.type";
	public final static String SETTING_MEAN_TYPE = "mean.type";
	public final static String SETTING_MAX_TYPE = "max.type";
	public final static String SETTING_MIN_VAL = "min.val";
	public final static String SETTING_MAX_VAL = "max.val";
	
	public final static int TYPE_ABSOLUTE = 1;
	public final static int TYPE_PERCENTILE = 2;
	
	public final static int MEAN_TYPE_ALL = 1;
	public final static int MEAN_TYPE_MINMAX = 2;
	
	public final static RGB DEFAULT_MIN_COLOR = new RGB(50,50,150);
	public final static RGB DEFAULT_MEAN_COLOR = new RGB(255,255,255);
	public final static RGB DEFAULT_MAX_COLOR = new RGB(150,50,50);
	
	public final static int DEFAULT_TYPE = TYPE_PERCENTILE;
	public final static int DEFAULT_MEAN_TYPE = MEAN_TYPE_ALL;
	
	public final static double DEFAULT_MIN_VAL = 5.0;
	public final static double DEFAULT_MAX_VAL = 95.0;
	
	private double min, mean, max;
	private int minType, meanType, maxType;
	
	private RGB minRGB, meanRGB, maxRGB;
	private RGB[] gradients;

	private IColorMethodData currentData;
	
	@Override
	public void configure(Map<String, String> settings) {
		minRGB = getSetting(SETTING_MIN_RGB, settings, DEFAULT_MIN_COLOR);
		meanRGB = getSetting(SETTING_MEAN_RGB, settings, DEFAULT_MEAN_COLOR);
		maxRGB = getSetting(SETTING_MAX_RGB, settings, DEFAULT_MAX_COLOR);
		minType = getSetting(SETTING_MIN_TYPE, settings, DEFAULT_TYPE);
		meanType = getSetting(SETTING_MEAN_TYPE, settings, DEFAULT_MEAN_TYPE);
		maxType = getSetting(SETTING_MAX_TYPE, settings, DEFAULT_TYPE);
		min = getSetting(SETTING_MIN_VAL, settings, DEFAULT_MIN_VAL);
		mean = 50.0;
		max = getSetting(SETTING_MAX_VAL, settings, DEFAULT_MAX_VAL);
		
		gradients = ColorUtils.createGradient(new RGB[]{minRGB, meanRGB, maxRGB}, 256);
	}
	
	@Override
	public void getConfiguration(Map<String, String> settings) {
		settings.put(SETTING_MIN_RGB, ColorUtils.createRGBString(minRGB));
		settings.put(SETTING_MEAN_RGB, ColorUtils.createRGBString(meanRGB));
		settings.put(SETTING_MAX_RGB, ColorUtils.createRGBString(maxRGB));
		settings.put(SETTING_MIN_TYPE, "" + minType);
		settings.put(SETTING_MEAN_TYPE, "" + meanType);
		settings.put(SETTING_MAX_TYPE, "" + maxType);
		settings.put(SETTING_MIN_VAL, "" + min);
		settings.put(SETTING_MAX_VAL, "" + max);
	}
	
	@Override
	public void initialize(IColorMethodData dataset) {
		currentData = dataset;
		if (dataset == null) {
			mean = 50.0;
		} else {
			if (meanType == MEAN_TYPE_ALL) mean = dataset.getMean();
			else mean = (getMinValue() + getMaxValue()) / 2;
		}
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
		
		//TODO Should legend scale be adjusted in case of percentiles? cfr. ManualMinMaxColorMethod
		
		return new LegendDrawer(orientation).getLegend(
				new RGB[] {minRGB, meanRGB, maxRGB},
				new String[] {"Min", "Mean", "Max"},
				new double[] {getMinValue(), mean, getMaxValue()},
				valueLabels,
				highlightValues,
				highlightLabels,
				width, height, isWhiteBackground);
	}
	
	@Override
	public BaseColorMethodDialog createDialog(Shell shell) {
		return new ManualMinMeanMaxColorMethodDialog(shell, this);
	}
	
	private int getIndex(double v) {
		double minValue = getMinValue();
		double maxValue = getMaxValue();
		
		double diff = mean - minValue;
		double ratio = ((gradients.length-1) / diff);
		int colorIndex = 0;

		if (Double.isNaN(v)) return -1;
		if (v <= minValue) colorIndex = 0;
		if (v < mean && v > minValue) colorIndex = (int) ((v - minValue) * ratio) / 2;

		diff = maxValue - mean;
		ratio = (255 / diff);

		if (v >= mean && v < maxValue) colorIndex = (int) ((v - mean) * ratio) / 2 + (gradients.length-1)/2;
		if (v >= maxValue) colorIndex = gradients.length-1;

		return colorIndex;
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