package eu.openanalytics.phaedra.wellimage;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;

import eu.openanalytics.phaedra.base.cache.CacheService;
import eu.openanalytics.phaedra.base.cache.ICache;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.wellimage.preferences.Prefs;
import eu.openanalytics.phaedra.wellimage.provider.SubWellImageProvider;
import eu.openanalytics.phaedra.wellimage.provider.WellImageProvider;

/**
 * API for rendering well and sub-well images.
 * Images can be rendered in various ways:
 * <ul>
 * <li>At full or reduced resolution (i.e. thumbnails)</li>
 * <li>Entirely or a specified region</li>
 * <li>Zoomed in or out</li>
 * <li>With all channels enabled or a specified set of channels</li>
 * </ul>
 * <p>
 * Images are rendered as SWT ImageData objects, which support various color modes and keep the pixel data in-memory.
 * </p>
 * <p>
 * All rendered images are cached, see {@link CacheService} for more information about caching.
 * </p>
 */
public class ImageRenderService {

	private static ImageRenderService instance = new ImageRenderService();

	private ICache wellCache;
	private ICache subwellCache;

	private WellImageProvider wellImageProvider;
	private SubWellImageProvider subwellImageProvider;

	private Collection<IPropertyChangeListener> propertyListeners;

	private ImageRenderService() {
		this.wellCache = CacheService.getInstance().createCache(WellImageProvider.class.getSimpleName());
		this.subwellCache = CacheService.getInstance().createCache(SubWellImageProvider.class.getSimpleName());

		this.subwellImageProvider = new SubWellImageProvider(subwellCache);
		this.wellImageProvider = new WellImageProvider(wellCache);

		this.propertyListeners = new HashSet<>();

		Activator.getDefault().getPreferenceStore().addPropertyChangeListener(e -> {
			String changedProperty = e.getProperty();
			if (isPaddingPropertyChanged(changedProperty)) {
				subwellImageProvider.reloadPreferencesAndClearCache();

				for (IPropertyChangeListener listener : propertyListeners) {
					listener.propertyChange(e);
				}
			}
		});
	}

	public static ImageRenderService getInstance() {
		return instance;
	}

	/**
	 * Get the size of the well image, adjusted by the given scale.
	 * 
	 * @param well The well whose image size will be retrieved.
	 * @param scale The scale to apply to the image size.
	 * @return The scaled well image size.
	 */
	public Point getWellImageSize(Well well, float scale) {
		return wellImageProvider.getWellImageSize(well, scale);
	}

	/**
	 * Check if a specific well image rendering is currently cached.
	 * 
	 * @param well The well the image belongs to.
	 * @param scale The scale at which the rendering was done.
	 * @param channels The channels that were enabled in the rendering.
	 * @return True if the rendering is cached.
	 */
	public boolean isWellImageCached(Well well, float scale, boolean[] channels) {
		return wellImageProvider.isWellImageCached(well, scale, channels);
	}

	/**
	 * Render a well image.
	 * 
	 * @param well The well whose image should be rendered.
	 * @param scale The scale at which the image should be rendered.
	 * @param channels The channels that should be included in the rendering.
	 * @return A rendering of the well image.
	 * @throws IOException If the well image cannot be read or rendered.
	 */
	public ImageData getWellImageData(Well well, float scale, boolean[] channels) throws IOException {
		return wellImageProvider.getWellImageData(well, scale, channels);
	}

	/**
	 * Render a well image.
	 * 
	 * @param well The well whose image should be rendered.
	 * @param w The width at which the image should be rendered.
	 * @param h The height at which the image should be rendered.
	 * @param channels The channels that should be included in the rendering.
	 * @return A rendering of the well image.
	 * @throws IOException If the well image cannot be read or rendered.
	 */
	public ImageData getWellImageData(Well well, int w, int h, boolean[] channels) throws IOException {
		return wellImageProvider.getWellImageData(well, w, h, channels);
	}

	/**
	 * Render a region of a well image.
	 * 
	 * @param well The well whose image should be rendered.
	 * @param scale The scale at which the image should be rendered.
	 * @param region The rectangular region of the image to render.
	 * @param channels The channels that should be included in the rendering.
	 * @return A rendering of the well image region.
	 * @throws IOException If the well image cannot be read or rendered.
	 */
	public ImageData getWellImageData(Well well, float scale, Rectangle region, boolean[] channels) throws IOException {
		return wellImageProvider.getWellImageData(well, scale, region, channels);
	}

	/**
	 * Get the aspect ratio of a plate's well images.
	 * Note that if a plate contains wells with different aspect ratios,
	 * an arbitrary well's image aspect ratio will be returned.
	 * 
	 * @param plate The plate whose well image ratio should be retrieved.
	 * @return The aspect ratio of the plate's well images, or 1 if the ratio cannot be determined.
	 */
	public float getWellImageAspectRatio(Plate plate) {
		return wellImageProvider.getImageAspectRatio(plate);
	}

	/**
	 * Get the aspect ratio of a list of well images.
	 * Note that if the list contains wells with different aspect ratios,
	 * an arbitrary well's image aspect ratio will be returned.
	 * 
	 * @param wells The wells whose image ratio should be retrieved.
	 * @return The aspect ratio of the well images, or 1 if the ratio cannot be determined.
	 */
	public float getWellImageAspectRatio(List<Well> wells) {
		return wellImageProvider.getImageAspectRatio(wells);
	}

	/**
	 * Remove all well image renderings from the cache for a given protocol class.
	 * 
	 * @param pClass The protocol class whose well images should be cleared from the cache.
	 */
	public void clearWellImageCache(ProtocolClass pClass) {
		clearImageCache(wellCache, pClass);
	}

	/**
	 * Retrieve the bounds of a subwell image.
	 * 
	 * @param well The well containing the subwell item.
	 * @param subwellIndex The index of the subwell item.
	 * @param scale The scale to apply to the image bounds.
	 * @return The scaled bounds of the subwell image.
	 */
	public Rectangle getSubWellImageBounds(Well well, int subwellIndex, float scale) {
		return subwellImageProvider.getSubWellImageBounds(well, subwellIndex, scale);
	}

	/**
	 * Check if a subwell image rendering is cached.
	 * 
	 * @param well The well containing the subwell item.
	 * @param subwellIndex The index of the subwell item.
	 * @param scale The scale at which the rendering was made.
	 * @param channels The channels that were enabled in the rendering.
	 * @return True if the subwell image rendering is cached.
	 */
	public boolean isSubWellImageCached(Well well, int subwellIndex, float scale, boolean[] channels) {
		return subwellImageProvider.isSubWellImageCached(well, subwellIndex, scale, channels);
	}

	/**
	 * Render a subwell image.
	 * 
	 * @param well The well containing the subwell item.
	 * @param subwellIndex The index of the subwell item.
	 * @param scale The scale at which the rendering should be done.
	 * @param channels The channels that should be included in the rendering.
	 * @return A rendering of the subwell image.
	 * @throws IOException If the subwell image cannot be read or rendered.
	 */
	public ImageData getSubWellImageData(Well well, int subwellIndex, float scale, boolean[] channels) throws IOException {
		return subwellImageProvider.getSubWellImageData(well, subwellIndex, scale, channels);
	}

	/**
	 * Remove all subwell image renderings from the cache for a given protocol class.
	 * 
	 * @param pClass The protocol class whose subwell images should be cleared from the cache.
	 */
	public void clearSubWellImageCache(ProtocolClass pClass) {
		clearImageCache(subwellCache, pClass);
	}

	/**
	 * Add a listener for changes to image rendering preferences.
	 * Note: this does not include image settings (gamma, contrast, etc).
	 * 
	 * @param listener The listener to be notified when image rendering preferences change.
	 */
	public void addImagePropertyChangeListener(IPropertyChangeListener listener) {
		propertyListeners.add(listener);
	}

	/**
	 * Remove a listener for changes to image rendering preferences.
	 * 
	 * @param listener The listener to remove from the list of listeners.
	 */
	public void removeImagePropertyChangeListener(IPropertyChangeListener listener) {
		propertyListeners.remove(listener);
	}

	private static boolean isPaddingPropertyChanged(String changedProperty) {
		return changedProperty.equals(Prefs.SW_IMG_IS_ABS_PADDING)
				|| changedProperty.equals(Prefs.SW_IMG_ABS_PADDING)
				|| changedProperty.equals(Prefs.SW_IMG_REL_PADDING);
	}

	private void clearImageCache(ICache imageCache, ProtocolClass pClass) {
		long pClassId = pClass.getId();
		imageCache.getKeys().stream()
			.filter(k -> k instanceof ImageKey)
			.filter(k -> ((ImageKey) k).getProtocolClassId() == pClassId)
			.forEach(k -> imageCache.remove(k));
	}

}
