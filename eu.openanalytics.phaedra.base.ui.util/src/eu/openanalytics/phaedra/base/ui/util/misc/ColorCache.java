package eu.openanalytics.phaedra.base.ui.util.misc;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;

import eu.openanalytics.phaedra.base.util.misc.ColorUtils;

public class ColorCache {

	public static Color get(int rgbInt) {
		RGB rgb = ColorUtils.hexToRgb(rgbInt);
		return get(rgb);
	}
	
	public static Color get(RGB rgb) {
		if (rgb == null) return null;
		String name = rgb.toString();
		if (!JFaceResources.getColorRegistry().hasValueFor(name)) JFaceResources.getColorRegistry().put(name, rgb);
		return get(name);
	}
	
	public static Color get(String name) {
		if (name == null) return null;
		return JFaceResources.getColorRegistry().get(name);
	}
}
