package eu.openanalytics.phaedra.base.util.misc;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;

/**
 * Utility class that converts RGB values to Color values,
 * and caches those Color values until the store is disposed.
 */
public class ColorStore {

	private Map<RGB, Color> colors;
	
	public ColorStore() {
		colors = new HashMap<RGB, Color>();
	}
	
	public Color get(RGB rgb) {
		Color c = colors.get(rgb);
		if (c == null) {
			c = new Color(null, rgb);
			colors.put(rgb, c);
		}
		return c;
	}
	
	public void dispose() {
		for (Color color: colors.values()) {
			color.dispose();
		}
	}
}
