package eu.openanalytics.phaedra.base.util.misc;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;

import org.eclipse.jface.resource.StringConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

/**
 * A collection of utilities for working with RGB color values.
 */
public class ColorUtils {

	/**
	 * Create a gradient between two colors, consisting of a specified number of gradient values.
	 */
	public static RGB[] createGradient(RGB start, RGB end, int values) {
		RGB[] colors = new RGB[values];
		int steps = values-1;

		float red = (float) (start.red - end.red) / steps;
		float green = (float) (start.green - end.green) / steps;
		float blue = (float) (start.blue - end.blue) / steps;

		for (int i = 0; i <= steps; i++) {
			int newRed = (int) (start.red - (red * i));
			int newGreen = (int) (start.green - (green * i));
			int newBlue = (int) (start.blue - (blue * i));
			colors[i] = new RGB(newRed, newGreen, newBlue);
		}
		
		return colors;
	}

	/**
	 * Create a chain of gradients between multiple colors, with a specified total number of gradient values.
	 */
	public static RGB[] createGradient(RGB[] colorSteps, int values) {
		RGB[] colors = new RGB[values];
		int colorBlocks = colorSteps.length - 1;
		int colorsPerBlock = values / colorBlocks;
		
		for (int i=0; i<colorBlocks; i++) {
			RGB[] subset = createGradient(colorSteps[i], colorSteps[i+1], colorsPerBlock);
			int offset = i*colorsPerBlock;
			System.arraycopy(subset, 0, colors, offset, colorsPerBlock);
		}
		
		if (colorsPerBlock*colorBlocks < values) {
			// Can happen if values / colorBlocks rounds down.
			colors[values-1] = colors[values-2];
		}
		
		return colors;
	}
	
	/**
	 * Convert an rgb integer value (highest byte, often the alpha, is not used) into an RGB object.
	 */
	public static RGB hexToRgb(int rgb) {
		int r = (rgb >> 16) & 0xFF;
		int g = (rgb >> 8) & 0xFF;
		int b = rgb & 0xFF;
		return new RGB(r, g, b);
	}
	
	/**
	 * Convert an RGB object into an rgb integer value. The highest integer byte, often the alpha, is not used.
	 */
	public static int rgbToHex(RGB rgb) {
		if (rgb == null) return 0;
		int intRgb = rgb.red;
		intRgb = (intRgb << 8) + rgb.green;
		intRgb = (intRgb << 8) + rgb.blue;
		return intRgb;
	}
	
	/**
	 * Convert an RGB object into a hexadecimal string.
	 * E.g. the color green becomes 0x00FF00.
	 */
	public static String rgbToStringHex(RGB rgb) {
		return Integer.toHexString(rgbToHex(rgb)).toUpperCase();
	}
	
	/**
	 * Parse an RGB value from a comma-separated string of 3 integer values (red,green,blue).
	 */
	public static RGB parseRGBString(String rgb) {
		if (rgb == null || rgb.isEmpty() || !rgb.contains(",")) return null;
		String[] parts = rgb.split(",");
		return new RGB(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
	}
	
	/**
	 * Convert an RGB value into a comma-separated string of 3 integer values (red,green,blue).
	 */
	public static String createRGBString(RGB rgb) {
		return StringConverter.asString(rgb);
	}
	
	/**
	 * Get a text color (black or white) that contrasts well with the given background color.
	 */
	public static Color getTextColor(RGB background) {
		if (background == null) return Display.getDefault().getSystemColor(SWT.COLOR_BLACK);
		double avg = (background.red + background.green + background.blue) / 3;
		if (avg > 127) return Display.getDefault().getSystemColor(SWT.COLOR_BLACK);
		else return Display.getDefault().getSystemColor(SWT.COLOR_WHITE);
	}
	
	/**
	 * Parse an RGB color from a String. Supported formats:
	 * 
	 * <ul>
	 * <li>Literal color names, e.g. "red", "blue", "cyan" etc.</li>
	 * <li>Decimal or hexadecimal colors, e.g. "65536", "#FF0000", "0x00FF00" etc.</li>
	 * <li>Comma-separated RGB values, e.g. "255,0,0", "0,128,128" etc.</li>
	 * </ul>
	 * 
	 * @param color The color String to parse.
	 * @return The RGB value, or null if the String could not be parsed.
	 */
	public static RGB parseColorString(String color) {
		RGB rgb = null;
		
		if (color == null || color.isEmpty()) return rgb;
		
		// First, try one of the SWT constants.
		try {
			Field f = SWT.class.getField("COLOR_" + color.toUpperCase());
			int colorCode = f.getInt(null);
			rgb = new RGB(0,0,0);
			final WeakReference<RGB> ref = new WeakReference<>(rgb);
			Display.getDefault().syncExec(() -> {
				RGB value = Display.getDefault().getSystemColor(colorCode).getRGB();
				ref.get().red = value.red;
				ref.get().green = value.green;
				ref.get().blue = value.blue;
			});
		} catch (Exception e) {}
		
		// Then, try to parse as a comma-separated r,g,b string
		if (rgb == null) {
			rgb = parseRGBString(color);
		}
		
		// Then, try to parse as a hex string
		if (rgb == null) {
			try {
				long colorValue = Long.decode(color);
				rgb = hexToRgb((int)colorValue);
			} catch (Exception e) {}
		}
		
		return rgb;
	}
}
