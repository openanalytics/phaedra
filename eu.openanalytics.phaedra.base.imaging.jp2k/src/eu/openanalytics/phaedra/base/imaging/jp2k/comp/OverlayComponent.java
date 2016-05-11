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

public class OverlayComponent extends BaseComponentType {

	private final static String CFG_COLORMASK = "colorMask";
	private final static int DEFAULT_COLORMASK = 0xFF0000;

	private int colorMask = DEFAULT_COLORMASK;

	@Override
	public String getName() {
		return "Overlay";
	}

	@Override
	public int getId() {
		return 1;
	}

	@Override
	public String getDescription() {
		return "An overlay component contains binary values representing areas\n(such as outlines) which are displayed with a fixed color.";
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

		int[] targetPixels = new int[target.width*target.height];
		target.getPixels(0, 0, targetPixels.length, targetPixels, 0);

		int alpha = params[5];
		for (int i=0; i<sourcePixels.length; i++) {
			int overlayValue = sourcePixels[i];
			// Skip background.
			if (overlayValue == 0) continue;
			// Skip transparent pixels.
			if (source.alphaData != null && source.alphaData[i] != (byte)255) continue;

			// Scaling 16bit down to 8bit usually results in labels betweeen 0 and 1.
			// So instead, chop off the highest 8 bits.
			if (source.depth == 16) overlayValue = overlayValue & 0xFF;
			else overlayValue = ImageUtils.to8bit(overlayValue, source.depth);

			if (overlayValue == 255) {
				if (alpha == 255) targetPixels[i] = colorMask;
				else targetPixels[i] = ImageUtils.blend(colorMask, targetPixels[i], alpha);
			} else {
				// There is already transparency in the overlay. Do not exceed it with custom alpha.
				if (alpha == 255) targetPixels[i] = ImageUtils.blend(colorMask, targetPixels[i], overlayValue);
				else targetPixels[i] = ImageUtils.blend(colorMask, targetPixels[i], Math.min(alpha,overlayValue));
			}
		}
		target.setPixels(0, 0, targetPixels.length, targetPixels, 0);
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
		gc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_WHITE));
		gc.fillRectangle(3, 3, 14, 14);
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
