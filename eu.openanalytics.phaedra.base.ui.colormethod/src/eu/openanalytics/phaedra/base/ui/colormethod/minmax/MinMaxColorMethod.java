package eu.openanalytics.phaedra.base.ui.colormethod.minmax;

import java.util.Map;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Shell;

import eu.openanalytics.phaedra.base.ui.colormethod.BaseColorMethod;
import eu.openanalytics.phaedra.base.ui.colormethod.BaseColorMethodDialog;
import eu.openanalytics.phaedra.base.ui.colormethod.IColorMethodData;
import eu.openanalytics.phaedra.base.ui.colormethod.LegendDrawer;
import eu.openanalytics.phaedra.base.util.misc.ColorUtils;

public class MinMaxColorMethod extends BaseColorMethod {

	private static final long serialVersionUID = 8277067727665961430L;
	
	public final static String SETTING_MIN_RGB = "min.rgb";
	public final static String SETTING_MAX_RGB = "max.rgb";
	
	public final static RGB DEFAULT_MIN_COLOR = new RGB(255,255,255);
	public final static RGB DEFAULT_MAX_COLOR = new RGB(0,180,0);
	
	private double min, max;
	private RGB minRGB, maxRGB;
	private RGB[] gradients;

	@Override
	public void configure(Map<String, String> settings) {
		if (settings != null) minRGB = ColorUtils.parseRGBString(settings.get(SETTING_MIN_RGB));
		if (minRGB == null) minRGB = DEFAULT_MIN_COLOR;
		if (settings != null) maxRGB = ColorUtils.parseRGBString(settings.get(SETTING_MAX_RGB));
		if (maxRGB == null) maxRGB = DEFAULT_MAX_COLOR;
		gradients = ColorUtils.createGradient(minRGB, maxRGB, 256);
	}
	
	@Override
	public void getConfiguration(Map<String, String> settings) {
		settings.put(SETTING_MIN_RGB, ColorUtils.createRGBString(minRGB));
		settings.put(SETTING_MAX_RGB, ColorUtils.createRGBString(maxRGB));
	}
	
	@Override
	public void initialize(IColorMethodData dataset) {
		if (dataset == null) {
			this.min = 0;
			this.max = 100;
		} else {
			this.min = dataset.getMin();
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
		if (labels) lbls = new String[] {"Min", "Max"};
		return new LegendDrawer(orientation).getLegend(
				new RGB[]{minRGB, maxRGB},
				lbls,
				new double[]{min, max},
				highlightValues,
				width, height, isWhiteBackground);
	}
	
	@Override
	public BaseColorMethodDialog createDialog(Shell shell) {
		return new MinMaxColorMethodDialog(shell, this);
	}
	
	public RGB getMinRGB() {
		return minRGB;
	}
	
	public void setMinRGB(RGB minRGB) {
		this.minRGB = minRGB;
	}
	
	public RGB getMaxRGB() {
		return maxRGB;
	}
	
	public void setMaxRGB(RGB maxRGB) {
		this.maxRGB = maxRGB;
	}
	
	private int getIndex(double v) {
		double diff = max - min;

		if (diff == 0 || Double.isNaN(v)) return -1;

		double ratio = (gradients.length-1) / diff;

		if (v < min) return 0;
		if (min <= v && v <= max) return (int)((v - min) * ratio);
		if (v > max) return gradients.length-1;
		
		return -1;
	}

}
