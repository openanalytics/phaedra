package eu.openanalytics.phaedra.wellimage.render;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;

import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.subwell.geometry.CalculatorFactory;
import eu.openanalytics.phaedra.wellimage.Activator;
import eu.openanalytics.phaedra.wellimage.preferences.Prefs;

public class ImageRenderRequestFactory {

	public static ImageRenderRequest.Builder forWell(Well well) {
		return requestBuilder().withWell(well);
	}
	
	public static ImageRenderRequest.Builder forSubWell(Well well, int subwellItem) {
		return requestBuilder().withWell(well).withRegion(getSubWellImageBounds(well, subwellItem));
	}
	
	public static ImageRenderRequest.Builder requestBuilder() {
		return new ImageRenderRequest.Builder();
	}
	
	private static Rectangle getSubWellImageBounds(Well well, int subwellIndex) {
		Rectangle rect = CalculatorFactory.getInstance().calculateBounds(well, subwellIndex);

		if (rect != null) {
			// Protect against memory issues by setting a maximum size.
			int maxSize = 1000000;
			if (rect.width * rect.height > maxSize) {
				rect.width = (int) Math.sqrt(maxSize);
				rect.height = (int) Math.sqrt(maxSize);
			}

			// Padding
			Point padding = new Point(0, 0);
			boolean useAbsolutePadding = Activator.getDefault().getPreferenceStore().getBoolean(Prefs.SW_IMG_IS_ABS_PADDING);
			if (useAbsolutePadding) {
				int p = Activator.getDefault().getPreferenceStore().getInt(Prefs.SW_IMG_ABS_PADDING);
				padding.x = p;
				padding.y = p;
			} else {
				float factor = ((float) Activator.getDefault().getPreferenceStore().getInt(Prefs.SW_IMG_REL_PADDING)) / 100;
				padding.x = (int) (rect.width * factor);
				padding.y = (int) (rect.height * factor);
			}
			
			rect.x -= padding.x;
			rect.y -= padding.y;
			rect.width += padding.x * 2;
			rect.height += padding.y * 2;

			if (rect.width == 0 || rect.height == 0) {
				int[] center = CalculatorFactory.getInstance().calculateCenter(well, subwellIndex);
				rect = new Rectangle(center[0]-25, center[1]-25, 50, 50);
			}
		} else {
			// Fallback.
			int[] center = CalculatorFactory.getInstance().calculateCenter(well, subwellIndex);
			if (center == null) rect = new Rectangle(0, 0, 1, 1);
			else rect = new Rectangle(center[0]-25, center[1]-25, 50, 50);
		}
		
		return new Rectangle(rect.x, rect.y, rect.width, rect.height);
	}
}
