package eu.openanalytics.phaedra.base.ui.colormethod.minmeanmax;

import java.util.Map;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Shell;

import eu.openanalytics.phaedra.base.ui.colormethod.BaseColorMethod;
import eu.openanalytics.phaedra.base.ui.colormethod.BaseColorMethodDialog;
import eu.openanalytics.phaedra.base.ui.colormethod.IColorMethodData;
import eu.openanalytics.phaedra.base.ui.colormethod.LegendDrawer;
import eu.openanalytics.phaedra.base.util.misc.ColorUtils;

public class MinMeanMaxColorMethod extends BaseColorMethod {

	private static final long serialVersionUID = -6661770030209640776L;
	
	public final static String SETTING_MIN_RGB = "min.rgb";
	public final static String SETTING_MEAN_RGB = "mean.rgb";
	public final static String SETTING_MAX_RGB = "max.rgb";
	
	public final static RGB DEFAULT_MIN_COLOR = new RGB(50,50,150);
	public final static RGB DEFAULT_MEAN_COLOR = new RGB(255,255,255);
	public final static RGB DEFAULT_MAX_COLOR = new RGB(150,50,50);
	
	private double min, mean, max;
	private RGB minRGB, meanRGB, maxRGB;
	private RGB[] gradients;

	@Override
	public void configure(Map<String, String> settings) {
		if (settings != null) {
			minRGB = ColorUtils.parseRGBString(settings.get(SETTING_MIN_RGB));
			meanRGB = ColorUtils.parseRGBString(settings.get(SETTING_MEAN_RGB));
			maxRGB = ColorUtils.parseRGBString(settings.get(SETTING_MAX_RGB));
		}
		if (minRGB == null) minRGB = DEFAULT_MIN_COLOR;
		if (meanRGB == null) meanRGB = DEFAULT_MEAN_COLOR;
		if (maxRGB == null) maxRGB = DEFAULT_MAX_COLOR;
		
		gradients = ColorUtils.createGradient(new RGB[]{minRGB, meanRGB, maxRGB}, 256);
	}
	
	@Override
	public void getConfiguration(Map<String, String> settings) {
		settings.put(SETTING_MIN_RGB, ColorUtils.createRGBString(minRGB));
		settings.put(SETTING_MEAN_RGB, ColorUtils.createRGBString(meanRGB));
		settings.put(SETTING_MAX_RGB, ColorUtils.createRGBString(maxRGB));
	}
	
	@Override
	public void initialize(IColorMethodData dataset) {
		if (dataset == null) {
			this.min = 0;
			this.mean = 50;
			this.max = 100;
		} else {
			this.min = dataset.getMin();
			this.mean = dataset.getMean();
			this.max = dataset.getMax();
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
		String[] lbls = new String[0];
		if (labels) lbls = new String[] {"Min", "Mean", "Max"};
		return new LegendDrawer(orientation).getLegend(
				new RGB[]{minRGB, meanRGB, maxRGB},
				lbls,
				new double[]{min, mean, max},
				highlightValues,
				width, height, isWhiteBackground);
	}
	
	@Override
	public BaseColorMethodDialog createDialog(Shell shell) {
		return new MinMeanMaxColorMethodDialog(shell, this);
	}
	
	public RGB getMinRGB() {
		return minRGB;
	}
	
	public void setMinRGB(RGB minRGB) {
		this.minRGB = minRGB;
	}
	
	public RGB getMeanRGB() {
		return meanRGB;
	}
	
	public void setMeanRGB(RGB meanRGB) {
		this.meanRGB = meanRGB;
	}
	
	public RGB getMaxRGB() {
		return maxRGB;
	}
	
	public void setMaxRGB(RGB maxRGB) {
		this.maxRGB = maxRGB;
	}
	
	private int getIndex(double v) {
		
		double diff = mean - min;
		double ratio = ((gradients.length-1) / diff);
		int colorIndex = 0;

		if (Double.isNaN(v)) return -1;
		if (v <= min) colorIndex = 0;
		if (v < mean && v > min) colorIndex = (int) ((v - min) * ratio) / 2;

		diff = max - mean;
		ratio = (255 / diff);

		if (v >= mean && v < max) colorIndex = (int) ((v - mean) * ratio) / 2 + (gradients.length-1)/2;
		if (v >= max) colorIndex = gradients.length-1;

		return colorIndex;
	}
}
