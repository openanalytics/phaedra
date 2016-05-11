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

import eu.openanalytics.phaedra.base.cache.CacheKey;
import eu.openanalytics.phaedra.base.cache.CacheService;
import eu.openanalytics.phaedra.base.util.misc.ColorUtils;
import eu.openanalytics.phaedra.base.util.misc.ImageUtils;

public class LookupComponent extends BaseComponentType {

	private final static String CFG_LOOKUP_LOW = "lookupLow";
	private final static String CFG_LOOKUP_HIGH = "lookupHigh";
	
	private final static int DEFAULT_LOOKUP_LOW = 0x00;
	private final static int DEFAULT_LOOKUP_HIGH = 0xFFFFFF;
	
	private int lookupLow = DEFAULT_LOOKUP_LOW;
	private int lookupHigh = DEFAULT_LOOKUP_HIGH;
	
	@Override
	public String getName() {
		return "Lookup Overlay";
	}

	@Override
	public int getId() {
		return 4;
	}

	@Override
	public String getDescription() {
		return "A lookup overlay contains pixel values (max value: 255),\nmapping to a lookup table generated from two colors.";
	}

	@Override
	public void loadConfig(Map<String, String> config) {
		lookupLow = loadColor(config, CFG_LOOKUP_LOW, DEFAULT_LOOKUP_LOW);
		lookupHigh = loadColor(config, CFG_LOOKUP_HIGH, DEFAULT_LOOKUP_HIGH);
	}
	
	@Override
	public void saveConfig(Map<String, String> config) {
		saveColor(config, CFG_LOOKUP_LOW, DEFAULT_LOOKUP_LOW);
		saveColor(config, CFG_LOOKUP_HIGH, DEFAULT_LOOKUP_HIGH);
	}
	
	@Override
	public void blend(ImageData source, ImageData target, int... params) {
		int[] sourcePixels = new int[target.width*target.height];
		source.getPixels(0, 0, sourcePixels.length, sourcePixels, 0);
		
		int[] targetPixels = new int[target.width*target.height];
		target.getPixels(0, 0, targetPixels.length, targetPixels, 0);
		
		int[] lookupTable = getLookupTable(lookupLow, lookupHigh);
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
			
			int color = lookupTable[overlayValue];
			if (alpha == 255) targetPixels[i] = color;
			else targetPixels[i] = ImageUtils.blend(color, targetPixels[i], alpha);
		}
		target.setPixels(0, 0, targetPixels.length, targetPixels, 0);
	}

	@Override
	public Image createIcon(Device device) {
		
		int r = (lookupLow & 0xFF0000) >> 16;
		int g = (lookupLow & 0x00FF00) >> 8;
		int b = lookupLow & 0x0000FF;
		Color low = new Color(device,r,g,b);
		r = (lookupHigh & 0xFF0000) >> 16;
		g = (lookupHigh & 0x00FF00) >> 8;
		b = lookupHigh & 0x0000FF;
		Color high = new Color(device,r,g,b);
		
		Image img = new Image(device, 20, 20);
		GC gc = new GC(img);
		gc.setForeground(low);
		gc.setBackground(high);
		gc.fillGradientRectangle(0, 0, 20, 20, true);
		gc.dispose();
		low.dispose();
		high.dispose();
		return img;
	}

	private int[] getLookupTable(int low, int high) {
		CacheKey key = CacheKey.create("LH-LUT", low, high);
		int[] table = (int[])CacheService.getInstance().getDefaultCache().get(key);
		if (table == null) {
			table = createLookupTable(low, high);
			CacheService.getInstance().getDefaultCache().put(key, table);
		}
		return table;
	}
	
	private int[] createLookupTable(int low, int high) {
		float lr, lg, lb, hr, hg, hb;
		lr = low >> 16;
		lg = (low >> 8) & 0xFF;
		lb = low & 0x0000FF;
		hr = high >> 16;
		hg = (high >> 8) & 0xFF;
		hb = high & 0x0000FF;
		
		int[] table = new int[256];
		for (int idx=0; idx < table.length; idx++) {
			table[idx] = ((int)((lr * (256-idx) + hr * idx) / 256.0) << 16) +
						((int)((lg * (256-idx) + hg * idx) / 256.0) << 8) +
						(int)((lb * (256-idx) + hb * idx) / 256.0);
		}
		return table;
	}
	
	@Override
	public void createConfigArea(final Composite parent, final Map<String, String> config, final ISelectionChangedListener changeListener) {

		RGB lowColor = loadColorRGB(config, CFG_LOOKUP_LOW, DEFAULT_LOOKUP_LOW);
		RGB highColor = loadColorRGB(config, CFG_LOOKUP_HIGH, DEFAULT_LOOKUP_HIGH);
		
		GridLayoutFactory.fillDefaults().numColumns(4).applyTo(parent);
		
		Label lbl = new Label(parent, SWT.NONE);
		lbl.setText("Low:");

		final ColorSelector lowSelector = new ColorSelector(parent);
		lowSelector.setColorValue(lowColor);
		
		lowSelector.addListener(new IPropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent event) {
				RGB newColor = lowSelector.getColorValue();
				config.put(CFG_LOOKUP_LOW, ColorUtils.createRGBString(newColor));
				if (changeListener != null) changeListener.selectionChanged(null);
			}
		});
		
		lbl = new Label(parent, SWT.NONE);
		lbl.setText("High:");
		
		final ColorSelector highSelector = new ColorSelector(parent);
		highSelector.setColorValue(highColor);
		
		highSelector.addListener(new IPropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent event) {
				RGB newColor = highSelector.getColorValue();
				config.put(CFG_LOOKUP_HIGH, ColorUtils.createRGBString(newColor));
				if (changeListener != null) changeListener.selectionChanged(null);
			}
		});
	}
}
