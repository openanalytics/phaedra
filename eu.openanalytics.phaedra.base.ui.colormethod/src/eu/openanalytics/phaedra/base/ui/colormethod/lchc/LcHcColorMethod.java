package eu.openanalytics.phaedra.base.ui.colormethod.lchc;

import java.util.Map;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Shell;

import eu.openanalytics.phaedra.base.ui.colormethod.BaseColorMethod;
import eu.openanalytics.phaedra.base.ui.colormethod.BaseColorMethodDialog;
import eu.openanalytics.phaedra.base.ui.colormethod.IColorMethodData;
import eu.openanalytics.phaedra.base.ui.colormethod.LegendDrawer;
import eu.openanalytics.phaedra.base.util.misc.ColorUtils;

public class LcHcColorMethod extends BaseColorMethod {

	private static final long serialVersionUID = -5995549417171324732L;
	
	public final static String SETTING_MIN_RGB = "min.rgb";
	public final static String SETTING_LC_RGB = "lc.rgb";
	public final static String SETTING_MEAN_RGB = "mean.rgb";
	public final static String SETTING_HC_RGB = "hc.rgb";
	public final static String SETTING_MAX_RGB = "max.rgb";
	
	public final static RGB DEFAULT_MIN_COLOR = new RGB(250,125,0);
	public final static RGB DEFAULT_LC_COLOR = new RGB(160,0,0);
	public final static RGB DEFAULT_MEAN_COLOR = new RGB(255,255,255);
	public final static RGB DEFAULT_HC_COLOR = new RGB(0,0,100);
	public final static RGB DEFAULT_MAX_COLOR = new RGB(0,0,230);
	
	private double min, lc, mean, hc, max;
	private RGB minRGB, lcRGB, meanRGB, hcRGB, maxRGB;
	private RGB[] gradients;
	
	@Override
	public void configure(Map<String, String> settings) {
		if (settings != null) minRGB = ColorUtils.parseRGBString(settings.get(SETTING_MIN_RGB));
		if (minRGB == null) minRGB = DEFAULT_MIN_COLOR;
		if (settings != null) lcRGB = ColorUtils.parseRGBString(settings.get(SETTING_LC_RGB));
		if (lcRGB == null) lcRGB = DEFAULT_LC_COLOR;
		if (settings != null) meanRGB = ColorUtils.parseRGBString(settings.get(SETTING_MEAN_RGB));
		if (meanRGB == null) meanRGB = DEFAULT_MEAN_COLOR;
		if (settings != null) hcRGB = ColorUtils.parseRGBString(settings.get(SETTING_HC_RGB));
		if (hcRGB == null) hcRGB = DEFAULT_HC_COLOR;
		if (settings != null) maxRGB = ColorUtils.parseRGBString(settings.get(SETTING_MAX_RGB));
		if (maxRGB == null) maxRGB = DEFAULT_MAX_COLOR;
		
		gradients = ColorUtils.createGradient(new RGB[]{minRGB, lcRGB, meanRGB, hcRGB, maxRGB}, 256);
	}

	@Override
	public void getConfiguration(Map<String, String> settings) {
		settings.put(SETTING_MIN_RGB, ColorUtils.createRGBString(minRGB));
		settings.put(SETTING_LC_RGB, ColorUtils.createRGBString(lcRGB));
		settings.put(SETTING_MEAN_RGB, ColorUtils.createRGBString(meanRGB));
		settings.put(SETTING_HC_RGB, ColorUtils.createRGBString(hcRGB));
		settings.put(SETTING_MAX_RGB, ColorUtils.createRGBString(maxRGB));
	}

	@Override
	public void initialize(IColorMethodData dataset) {
		if (dataset == null) {
			this.min = 0;
			this.lc = 25;
			this.mean = 50;
			this.hc = 75;
			this.max = 100;
		} else {
			this.min = dataset.getMin();
			this.lc = dataset.getValue("lc");
			this.mean = dataset.getMean();
			this.hc = dataset.getValue("hc");
			this.max = dataset.getMax();
		}
	}
	
	@Override
	public Image getLegend(int width, int height, int orientation, boolean labels, double[] highlightValues) {
		return getLegend(width, height, orientation, labels, highlightValues, false);
	}
	
	@Override
	public Image getLegend(int width, int height, int orientation, boolean labels, double[] highlightValues, boolean isWhiteBackground) {
		String[] lbls = new String[0];
		if (labels) lbls = new String[] {"Min", "LC", "Mean", "HC", "Max"};
		return new LegendDrawer(orientation).getLegend(
				new RGB[]{minRGB, lcRGB, meanRGB, hcRGB, maxRGB},
				lbls,
				new double[]{min, lc, mean, hc, max},
				highlightValues,
				width, height, isWhiteBackground);
	}

	@Override
	public BaseColorMethodDialog createDialog(Shell shell) {
		return new LcHcColorMethodDialog(shell, this);
	}

	@Override
	public RGB getColor(double v) {
		int index = getIndex(v);
		if (index == -1) return null;
		return gradients[index];
	}
	
	private int getIndex(double v) {
		
		if (v <= min) return 0;
		if (v < lc) return (int)(((v-min)/(lc-min))*64);
		if (v < mean) return 64+(int)(((v-lc)/(mean-lc))*64);
		if (v < hc) return 128+(int)(((v-mean)/(hc-mean))*64);
		if (v < max) return 192+(int)(((v-hc)/(max-hc))*64);
		if (v >= max) return 255;
		
		return -1;
	}

	public RGB getMinRGB() {
		return minRGB;
	}
	
	public void setMinRGB(RGB minRGB) {
		this.minRGB = minRGB;
	}
	
	public RGB getLcRGB() {
		return lcRGB;
	}
	
	public void setLcRGB(RGB lcRGB) {
		this.lcRGB = lcRGB;
	}
	
	public RGB getMeanRGB() {
		return meanRGB;
	}
	
	public void setMeanRGB(RGB meanRGB) {
		this.meanRGB = meanRGB;
	}
	
	public RGB getHcRGB() {
		return hcRGB;
	}
	
	public void setHcRGB(RGB hcRGB) {
		this.hcRGB = hcRGB;
	}
	
	public RGB getMaxRGB() {
		return maxRGB;
	}
	
	public void setMaxRGB(RGB maxRGB) {
		this.maxRGB = maxRGB;
	}
}
