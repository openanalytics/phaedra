package eu.openanalytics.phaedra.wellimage;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;

import eu.openanalytics.phaedra.base.cache.CacheService;
import eu.openanalytics.phaedra.base.cache.ICache;
import eu.openanalytics.phaedra.base.event.ModelEventService;
import eu.openanalytics.phaedra.base.event.ModelEventType;
import eu.openanalytics.phaedra.base.imaging.jp2k.IDecodeAPI;
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.wellimage.preferences.Prefs;
import eu.openanalytics.phaedra.wellimage.render.DecoderPool;
import eu.openanalytics.phaedra.wellimage.render.ImageRenderRequest;
import eu.openanalytics.phaedra.wellimage.render.ImageRenderRequestFactory;
import eu.openanalytics.phaedra.wellimage.render.ImageRenderer;

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
	
	private DecoderPool decoderPool;
	private ExecutorService executor;
	private ICache imageCache;
	
	private Collection<IPropertyChangeListener> propertyListeners;
	
	private ImageRenderService() {
		decoderPool = new DecoderPool(4, 300000L);
		executor = Executors.newCachedThreadPool();
		imageCache = CacheService.getInstance().createCache("ImageCache");
		
		this.propertyListeners = new HashSet<>();
		Activator.getDefault().getPreferenceStore().addPropertyChangeListener(e -> {
			String changedProperty = e.getProperty();
			if (changedProperty.equals(Prefs.SW_IMG_IS_ABS_PADDING)
					|| changedProperty.equals(Prefs.SW_IMG_ABS_PADDING)
					|| changedProperty.equals(Prefs.SW_IMG_REL_PADDING)) {
				imageCache.clear();
				for (IPropertyChangeListener listener : propertyListeners) {
					listener.propertyChange(e);
				}
			}
		});
		
		ModelEventService.getInstance().addEventListener(e -> {
			if (e.type == ModelEventType.ObjectAboutToBeRemoved && e.source instanceof Plate) {
				Plate plate = (Plate) e.source;
				clearCache(plate);
				releaseDecoders(plate);
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
	 * @return The scaled well image size, or (0,0) if the size could not be determined.
	 */
	public Point getWellImageSize(Well well, float scale) {
		String key = "WellImageSize" + well.getId() + "#" + scale;
		Point size = (Point) imageCache.get(key);
		if (size == null) {
			try {
				int imageNr = PlateUtils.getWellNr(well) - 1;
				Point fullImageSize = useDecoder(well.getPlate(), dc -> dc.getSize(imageNr));
				size = new Point((int) (fullImageSize.x * scale), (int) (fullImageSize.y * scale));
			} catch (IOException e) {
				size = new Point(0, 0);
			}
			imageCache.put(key, size);
		}
		return new Point(size.x, size.y);
	}
	
	/**
	 * Get the bit depth of a channel of a well image.
	 * Note that different channels may have different bit depths.
	 * 
	 * @param well The well whose image will be used.
	 * @param channelNr The 0-based index of the channel whose bit depth will be retrieved.
	 * @return The bit depth for the given channel, or 0 if the bit depth could not be determined.
	 */
	public int getWellImageDepth(Well well, int channelNr) {
		try {
			int imageNr = PlateUtils.getWellNr(well) - 1;
			return useDecoder(well.getPlate(), dc -> dc.getBitDepth(imageNr, channelNr));
		} catch (IOException e) {
			return 0;
		}
	}
	
	/**
	 * Get the rectangle that contains the given subwell item on the full well image.
	 * Note that this may include some padding pixels, depending on user preferences.
	 * 
	 * @param well The well containing the subwell item whose bounds will be calculated.
	 * @param subwellIndex The 0-based index of the subwell item whose bounds will be calculated.
	 * @param scale The scale to apply to the bounds.
	 * @return A Rectangle representing the bounds of the given subwell item.
	 */
	public Rectangle getSubWellImageBounds(Well well, int subwellIndex, float scale) {
		Rectangle region = ImageRenderRequestFactory.forSubWell(well, subwellIndex).build().region;
		return new Rectangle(region.x, region.y, Math.round(region.width * scale), Math.round(region.height * scale));
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
//		try {
//			// In many cases, B2 is a safe well to test against.
//			Well sampleWell = PlateUtils.getWell(plate, NumberUtils.getWellNr("B2", plate.getColumns()));
//			return getWellImageAspectRatio(Collections.singletonList(sampleWell));
//		} catch(Exception e) {
//			return getWellImageAspectRatio(plate.getWells());
//		}
		return getWellImageAspectRatio(plate.getWells());
	}

	/**
	 * Get the aspect ratio of a set of well images.
	 * Note that if the set contains wells with different aspect ratios,
	 * an arbitrary well's image aspect ratio will be returned.
	 * 
	 * @param wells The set of wells whose image ratio should be retrieved.
	 * @return The aspect ratio of the well images, or 1 if the ratio cannot be determined.
	 */
	public float getWellImageAspectRatio(List<Well> wells) {
		for (Well well: wells) {
			Point size = getWellImageSize(well, 1f);
			if (size.x != 0 && size.y != 0) return size.x / (float) size.y;
		}
		return 1.0f;
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
		ImageRenderRequest req = ImageRenderRequestFactory.forWell(well).withScale(scale).withComponents(channels).build();
		Object key = getCacheKey(req);
		return imageCache.contains(key);
	}
	
	/**
	 * Check if a specific subwell image rendering is currently cached.
	 * 
	 * @param well The well the image belongs to.
	 * @param subwellIndex The 0-based index of the subwell item that must be checked.
	 * @param scale The scale at which the rendering was done.
	 * @param channels The channels that were enabled in the rendering.
	 * @return True if the rendering is cached.
	 */
	public boolean isSubWellImageCached(Well well, int subwellIndex, float scale, boolean[] channels) {
		ImageRenderRequest req = ImageRenderRequestFactory.forSubWell(well, subwellIndex).withScale(scale).withComponents(channels).build();
		Object key = getCacheKey(req);
		return imageCache.contains(key);
	}
	
	/**
	 * Remove all cached images for the given protocol class.
	 * 
	 * @param pClass The protocol class whose cached images will be cleared.
	 */
	public void clearCache(ProtocolClass pClass) {
		long pClassId = pClass.getId();
		imageCache.getKeys().stream()
			.filter(k -> k instanceof ImageKey)
			.filter(k -> ((ImageKey) k).getProtocolClassId() == pClassId)
			.forEach(k -> imageCache.remove(k));
	}
	
	/**
	 * Remove all cached images for the given plate.
	 * 
	 * @param plate The plate whose cached images will be cleared.
	 */
	public void clearCache(Plate plate) {
		List<Long> wellIds = PlateService.streamableList(plate.getWells()).stream().map(w -> w.getId()).collect(Collectors.toList());
		imageCache.getKeys().stream()
			.filter(k -> k instanceof ImageKey)
			.filter(k -> wellIds.contains(((ImageKey) k).getWellId()))
			.forEach(k -> imageCache.remove(k));
	}
	
	/**
	 * Render a well image.
	 * 
	 * @param well The well whose image should be rendered.
	 * @param scale The scale at which the image should be rendered.
	 * @param channels The channels that should be included in the rendering.
	 * @return A rendering of the well image region.
	 * @throws IOException If the well image cannot be read or rendered.
	 */
	public ImageData getWellImageData(Well well, float scale, boolean[] channels) throws IOException {
		ImageRenderRequest req = ImageRenderRequestFactory.forWell(well).withScale(scale).withComponents(channels).build();
		return getImageData(req);
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
		ImageRenderRequest req = ImageRenderRequestFactory.forWell(well).withSize(new Point(w, h)).withComponents(channels).build();
		return getImageData(req);
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
		ImageRenderRequest req = ImageRenderRequestFactory.forWell(well).withScale(scale).withRegion(region).withComponents(channels).build();
		return getImageData(req);
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
		ImageRenderRequest req = ImageRenderRequestFactory.forSubWell(well, subwellIndex).withScale(scale).withComponents(channels).build();
		return getImageData(req);
	}
	
	/**
	 * Render an image.
	 * 
	 * @param req A custom image rendering request.
	 * @return A rendering of the image.
	 * @throws IOException If the image cannot be read or rendered.
	 */
	public ImageData getImageData(ImageRenderRequest req) throws IOException {
		Object key = getCacheKey(req);
		ImageData cachedData = (ImageData) imageCache.get(key);
		if (cachedData == null) {
			cachedData = renderImage(req);
			imageCache.put(key, cachedData);
		}
		return cachedData;
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
	
	/**
	 * Close any resources that may be currently held for rendering images of the given plate.
	 * Decoders may be cached for a while, to increase performance. Calling this method will
	 * ensure that no resources (e.g. file handles) are held, and allows for moving/deleting
	 * the plate.
	 * 
	 * @param plate The plate whose resources will be closed.
	 */
	public void releaseDecoders(Plate plate) {
		decoderPool.clear(plate);
	}
	
	private ImageData renderImage(ImageRenderRequest req) throws IOException {
		return useDecoder(req.well.getPlate(), dc -> new ImageRenderer().render(req, dc));
	}
	
	private <T> T useDecoder(Plate plate, DecodeOperation<T> operation) throws IOException {
		try {
			return executor.submit(() -> {
				IDecodeAPI decoder = decoderPool.borrowObject(plate);
				try {
					return operation.apply(decoder);
				} finally {
					decoderPool.returnObject(plate, decoder);
				}
			}).get();
		} catch (Throwable e) {
			if (e.getCause() instanceof IOException) throw (IOException) e.getCause();
			else throw new IOException("Failed to decode image", e);
		}
	}
	
	private static interface DecodeOperation<T> {
		public T apply(IDecodeAPI decoder) throws IOException;
	}
	
	private Object getCacheKey(ImageRenderRequest req) {
		if (req.customSettings != null) return null;
		if (req.region != null) return new ImageKey(req.well, req.scale, req.components, req.region);
		else if (req.size != null) return new ImageKey(req.well, req.size.x, req.size.y, req.components);
		else if (req.scale != 0.0f) return new ImageKey(req.well, req.scale, req.components);
		else return null;
	}

}
