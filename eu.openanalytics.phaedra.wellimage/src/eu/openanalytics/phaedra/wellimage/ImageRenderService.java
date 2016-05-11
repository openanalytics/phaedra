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

	/*
	 * ***********
	 * Well Images
	 * ***********
	 */

	/**
	 * <p>Returns the Well Image size using the given scale.
	 * </p>
	 * @param well
	 * @param scale
	 * @return
	 */
	public Point getWellImageSize(Well well, float scale) {
		return wellImageProvider.getWellImageSize(well, scale);
	}

	/**
	 * <p>Check if the specified Well Image is cached.
	 * </p>
	 * @param well
	 * @param scale
	 * @param channels
	 * @return
	 */
	public boolean isWellImageCached(Well well, float scale, boolean[] channels) {
		return wellImageProvider.isWellImageCached(well, scale, channels);
	}

	/**
	 * <p>Get the full Well Image at the given scale.
	 * </p>
	 * @param well
	 * @param scale
	 * @param channels
	 * @return
	 * @throws IOException
	 */
	public ImageData getWellImageData(Well well, float scale, boolean[] channels) throws IOException {
		return wellImageProvider.getWellImageData(well, scale, channels);
	}

	/**
	 * <p>Get the full Well Image at given width and height.
	 * </p>
	 * @param well
	 * @param w
	 * @param h
	 * @param channels
	 * @return
	 * @throws IOException
	 */
	public ImageData getWellImageData(Well well, int w, int h, boolean[] channels) throws IOException {
		return wellImageProvider.getWellImageData(well, w, h, channels);
	}

	/**
	 * <p>Get the given region from the Well Image at the given scale.
	 * </p>
	 * @param well
	 * @param scale
	 * @param region
	 * @param channels
	 * @return
	 * @throws IOException
	 */
	public ImageData getWellImageData(Well well, float scale, Rectangle region, boolean[] channels) throws IOException {
		return wellImageProvider.getWellImageData(well, scale, region, channels);
	}

	public float getWellImageAspectRatio(Plate plate) {
		return wellImageProvider.getImageAspectRatio(plate);
	}

	public float getWellImageAspectRatio(List<Well> wells) {
		return wellImageProvider.getImageAspectRatio(wells);
	}

	public void clearWellImageCache(ProtocolClass pClass) {
		clearImageCache(wellCache, pClass);
	}

	/*
	 * **************
	 * SubWell Images
	 * **************
	 */

	public Rectangle getSubWellImageBounds(Well well, int cellIndex, float scale) {
		return subwellImageProvider.getSubWellImageBounds(well, cellIndex, scale);
	}

	public boolean isSubWellImageCached(Well well, int cellIndex, float scale, boolean[] channels) {
		return subwellImageProvider.isSubWellImageCached(well, cellIndex, scale, channels);
	}

	public ImageData getSubWellImageData(Well well, int cellIndex, float scale, boolean[] channels) throws IOException {
		return subwellImageProvider.getSubWellImageData(well, cellIndex, scale, channels);
	}

	public void clearSubWellImageCache(ProtocolClass pClass) {
		clearImageCache(subwellCache, pClass);
	}

	/*
	 * ******************
	 * Property Listeners
	 * ******************
	 */

	public void addImagePropertyChangeListener(IPropertyChangeListener listener) {
		propertyListeners.add(listener);
	}

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
