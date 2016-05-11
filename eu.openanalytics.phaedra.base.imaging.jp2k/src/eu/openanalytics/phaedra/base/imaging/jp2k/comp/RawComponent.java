package eu.openanalytics.phaedra.base.imaging.jp2k.comp;

import java.util.Map;

import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.ColorSelector;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.util.misc.ColorUtils;
import eu.openanalytics.phaedra.base.util.misc.ImageUtils;

public class RawComponent extends BaseComponentType {

	private final static String CFG_COLORMASK = "colorMask";
	private final static int DEFAULT_COLORMASK = 0xFFFFFF;

	private int colorMask = DEFAULT_COLORMASK;

	@Override
	public String getName() {
		return "Raw";
	}

	@Override
	public int getId() {
		return 0;
	}

	@Override
	public String getDescription() {
		return "A raw component contains image pixels in one color dimension,\nusually represented with a greyscale palette.";
	}

	@Override
	public void loadConfig(Map<String, String> config) {
		colorMask = loadColor(config, CFG_COLORMASK, DEFAULT_COLORMASK);
	}

	@Override
	public void saveConfig(Map<String, String> config) {
		saveColor(config, CFG_COLORMASK, colorMask);
	}

	@Override
	public void blend(ImageData source, ImageData target, int... params) {
		int[] sourcePixels = new int[target.width*target.height];
		source.getPixels(0, 0, sourcePixels.length, sourcePixels, 0);

		int[] destPixels = new int[target.width*target.height];
		target.getPixels(0, 0, destPixels.length, destPixels, 0);

		int lvlMin = params[3];
		int lvlMax = params[4];

		for (int i=0; i<sourcePixels.length; i++) {

			// Transfer alpha value to the target.
			if (source.alphaData != null && target.alphaData != null) target.alphaData[i] = source.alphaData[i];

			int destValue = destPixels[i];
			int srcValue = sourcePixels[i];

			int grayValue = ImageUtils.to8bit(srcValue, source.depth, lvlMin, lvlMax);

			int maskR = (colorMask & 0xFF0000) >> 16;
			int maskG = (colorMask & 0xFF00) >> 8;
			int maskB = (colorMask & 0xFF);

			// Convert to HSB, adjust brightness, convert back to RGB
			float[] colorMaskHSB = java.awt.Color.RGBtoHSB(maskR, maskG, maskB, null);
			colorMaskHSB[2] = colorMaskHSB[2]*(grayValue/255.0f);
			srcValue = java.awt.Color.HSBtoRGB(colorMaskHSB[0], colorMaskHSB[1], colorMaskHSB[2]);

			// This method is faster, but may procude incorrect colors:
//			// Put the gray value in each channel slot (R,G,B). This will allow color masking.
//			srcValue = grayValue + (grayValue << 8) + (grayValue << 16);
//			srcValue = srcValue & colorMask;

			int srcR = (srcValue & 0xFF0000) >> 16;
			int srcG = (srcValue & 0xFF00) >> 8;
			int srcB = (srcValue & 0xFF);

			int destR = (destValue & 0xFF0000) >> 16;
			int destG = (destValue & 0xFF00) >> 8;
			int destB = (destValue & 0xFF);

			// The maximum of the two pixels is taken.
			int newR = Math.max(destR,srcR);
			int newG = Math.max(destG,srcG);
			int newB = Math.max(destB,srcB);
			int newValue = (newR << 16) | (newG << 8) | newB;

			destPixels[i] = newValue;
		}

		target.setPixels(0, 0, destPixels.length, destPixels, 0);
	}

	@Override
	public Image createIcon(Device device) {
		int r = (colorMask & 0xFF0000) >> 16;
		int g = (colorMask & 0x00FF00) >> 8;
		int b = colorMask & 0x0000FF;
		Image img = new Image(device, 20, 20);
		GC gc = new GC(img);
		Color color = new Color(device,r,g,b);
		gc.setBackground(color);
		gc.fillRectangle(0, 0, 20, 20);
		color.dispose();
		gc.dispose();
		return img;
	}

	@Override
	public void createConfigArea(Composite parent, final Map<String, String> config, final ISelectionChangedListener changeListener) {

		RGB currentColor = loadColorRGB(config, CFG_COLORMASK, DEFAULT_COLORMASK);

		GridLayoutFactory.fillDefaults().numColumns(3).applyTo(parent);

		Label lbl = new Label(parent, SWT.NONE);
		lbl.setImage(IconManager.getIconImage("color_wheel.png"));

		final ColorSelector selector = new ColorSelector(parent);
		selector.setColorValue(currentColor);

		selector.addListener(new IPropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent event) {
				RGB newColor = selector.getColorValue();
				config.put(CFG_COLORMASK, ColorUtils.createRGBString(newColor));
				if (changeListener != null) changeListener.selectionChanged(null);
			}
		});
	}
}