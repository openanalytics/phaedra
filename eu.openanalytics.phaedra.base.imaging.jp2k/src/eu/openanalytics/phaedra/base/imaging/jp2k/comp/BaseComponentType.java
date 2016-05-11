package eu.openanalytics.phaedra.base.imaging.jp2k.comp;

import java.util.Map;

import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import eu.openanalytics.phaedra.base.util.misc.ColorUtils;

public abstract class BaseComponentType implements IComponentType {

	@Override
	public String getDescription() {
		return "No description available";
	}

	@Override
	public void loadConfig(Map<String, String> config) {
		// No configuration needed.
	}
	
	@Override
	public void saveConfig(Map<String, String> config) {
		// No configuration to save.
	}
	
	@Override
	public void createConfigArea(Composite parent, Map<String, String> config, ISelectionChangedListener changeListener) {
		// Default: no configuration needed.
		Label lbl = new Label(parent, SWT.NONE);
		lbl.setText("<Not applicable>");
		lbl.setEnabled(false);
	}
	
	protected static int loadColor(Map<String,String> config, String key, int defaultValue) {
		return ColorUtils.rgbToHex(loadColorRGB(config, key, defaultValue));
	}
	
	protected static RGB loadColorRGB(Map<String,String> config, String key, int defaultValue) {
		String colorString = config.get(key);
		RGB currentColor = ColorUtils.parseRGBString(colorString);
		if (currentColor != null) return currentColor;
		else return ColorUtils.hexToRgb(defaultValue);
	}
	
	protected static void saveColor(Map<String,String> config, String key, int value) {
		String colorString = ColorUtils.createRGBString(ColorUtils.hexToRgb(value));
		config.put(key, colorString);
	}
}
