package eu.openanalytics.phaedra.wellimage.provider;

import java.io.IOException;

import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Rectangle;

import eu.openanalytics.phaedra.base.cache.ICache;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.vo.ImageSettings;
import eu.openanalytics.phaedra.model.subwell.geometry.CalculatorFactory;
import eu.openanalytics.phaedra.wellimage.Activator;
import eu.openanalytics.phaedra.wellimage.ImageKey;
import eu.openanalytics.phaedra.wellimage.ImageRenderService;
import eu.openanalytics.phaedra.wellimage.ImageSettingsFactory;
import eu.openanalytics.phaedra.wellimage.preferences.Prefs;

public class SubWellImageProvider {

	private static final String SUBWELL_IMG_SIZE_ = "SUBWELL_IMG_SIZE_";

	private ICache cache;

	private boolean isAbsolute;
	private int absolutePadding;
	private float relativePadding;

	/**
	 * <p>Should only be used by {@link ImageRenderService}.
	 * </p>
	 * @param cache
	 */
	public SubWellImageProvider(ICache cache) {
		this.cache = cache;

		loadPreferences();
	}

	public Rectangle getSubWellImageBounds(Well well, int cellIndex, float scale) {
		Object key = SUBWELL_IMG_SIZE_ + well.getId() + "@" + cellIndex;
		Object o = cache.get(key);
		if (o == null) {
			Rectangle rect = CalculatorFactory.getInstance().calculateBounds(well, cellIndex);

			if (rect != null) {
				// Protect against memory issues by setting a maximum size.
				int maxSize = 1000000;
				if (rect.width*rect.height > maxSize) {
					rect.width = (int)Math.sqrt(maxSize);
					rect.height = (int)Math.sqrt(maxSize);
				}

				// Padding
				int paddingW;
				int paddingH;
				if (isAbsolute) {
					paddingW = absolutePadding;
					paddingH = absolutePadding;
				} else {
					paddingW = (int) (rect.width * relativePadding);
					paddingH = (int) (rect.height * relativePadding);
				}
				rect.x -= paddingW;
				rect.y -= paddingH;
				rect.width += paddingW * 2;
				rect.height += paddingH * 2;

				if (rect.width == 0 || rect.height == 0) {
					int[] center = CalculatorFactory.getInstance().calculateCenter(well, cellIndex);
					rect = new Rectangle(center[0]-25, center[1]-25, 50, 50);
				}
				o = cache.put(key, rect);
			} else {
				// Fallback.
				int[] center = CalculatorFactory.getInstance().calculateCenter(well, cellIndex);
				if (center == null) rect = new Rectangle(0, 0, 1, 1);
				else rect = new Rectangle(center[0]-25, center[1]-25, 50, 50);

				o = cache.put(key, rect);
			}
		}
		// Return the requested scale.
		Rectangle rect = (Rectangle) o;
		// The width/height could be 1px larger than the actual image since Kakadu does not round .5 consistently.
		return new Rectangle(rect.x, rect.y, Math.round(rect.width * scale), Math.round(rect.height * scale));
	}

	public boolean isSubWellImageCached(Well well, int cellIndex, float scale, boolean[] channels) {
		ImageKey key = getKey(well, cellIndex, scale, channels);
		return cache.contains(key);
	}

	public ImageData getSubWellImageData(Well well, int cellIndex, float scale, boolean[] channels) throws IOException {
		ImageKey key = getKey(well, cellIndex, scale, channels);
		ImageData render = (ImageData) cache.get(key);
		if (render == null) {
			try (ImageProvider imageProvider = new ImageProvider(well.getPlate())) {
				imageProvider.open();

				ImageSettings currentSettings = ImageSettingsFactory.getSettings(well);
				if (channels == null || channels.length != currentSettings.getImageChannels().size()) {
					channels = new boolean[currentSettings.getImageChannels().size()];
					for (int i=0; i<channels.length; i++) {
						channels[i] = currentSettings.getImageChannels().get(i).isShowInWellView();
					}
				}

				// Use a scale of 1 since the render method already decides upon the size.
				Rectangle rect = getSubWellImageBounds(well, cellIndex, 1f);
				int nr = PlateUtils.getWellNr(well);

				render = imageProvider.render(currentSettings, scale, rect, nr-1, channels);
				cache.put(key, render);
			}
		}
		return render;
	}

	/**
	 * <p>Reload the preferences and clear the used cache.</p>
	 */
	public void reloadPreferencesAndClearCache() {
		loadPreferences();

		this.cache.clear();
	}

	private void loadPreferences() {
		this.isAbsolute = Activator.getDefault().getPreferenceStore().getBoolean(Prefs.SW_IMG_IS_ABS_PADDING);
		this.absolutePadding = Activator.getDefault().getPreferenceStore().getInt(Prefs.SW_IMG_ABS_PADDING);
		int rel = Activator.getDefault().getPreferenceStore().getInt(Prefs.SW_IMG_REL_PADDING);
		this.relativePadding = (float) rel / 100;
	}

	private ImageKey getKey(Well well, int cellIndex, float scale, boolean[] channels) {
		return new ImageKey(well, cellIndex, scale, channels);
	}

}