package eu.openanalytics.phaedra.wellimage.provider;

import java.io.IOException;
import java.util.List;

import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;

import eu.openanalytics.phaedra.base.cache.ICache;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.vo.ImageSettings;
import eu.openanalytics.phaedra.wellimage.ImageKey;
import eu.openanalytics.phaedra.wellimage.ImageRenderService;
import eu.openanalytics.phaedra.wellimage.ImageSettingsFactory;

public class WellImageProvider {

	private static final String WELL_IMG_SIZE_ = "WELL_IMG_SIZE_";

	private ICache cache;

	/**
	 * <p>Should only be used by {@link ImageRenderService}.
	 * </p>
	 * @param cache
	 */
	public WellImageProvider(ICache cache) {
		this.cache = cache;
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
		String key = WELL_IMG_SIZE_ + well.getId();
		Object o = cache.get(key);
		if (o == null) {
			int nr = PlateUtils.getWellNr(well);

			try (ImageProvider imageProvider = new ImageProvider(well.getPlate())) {
				imageProvider.open();
				o = cache.put(key, imageProvider.getImageSize(nr-1));
			} catch (IOException e) {
				o = cache.put(key, new Point(0, 0));
			}
		}
		Point imageSize = (Point) o;
		Point scaledSize = new Point((int) (imageSize.x * scale), (int) (imageSize.y * scale));
		return scaledSize;
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
		Object key = getKey(well, scale, channels);
		return cache.contains(key);
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
		Object key = getKey(well, scale, channels);
		ImageData imageData = (ImageData) cache.get(key);
		if (imageData == null) {
			try (ImageProvider imageProvider = new ImageProvider(well.getPlate())) {
				imageProvider.open();

				ImageSettings currentSettings = ImageSettingsFactory.getSettings(well);
				channels = checkChannels(channels, currentSettings);

				int nr = PlateUtils.getWellNr(well);

				imageData = imageProvider.render(currentSettings, scale, nr-1, channels);
				cache.put(key, imageData);
			}
		}
		return imageData;
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
		Object key = getKey(well, w, h, channels);
		ImageData imageData = (ImageData) cache.get(key);
		if (imageData == null) {
			try (ImageProvider imageProvider = new ImageProvider(well.getPlate())) {
				imageProvider.open();

				ImageSettings currentSettings = ImageSettingsFactory.getSettings(well);
				channels = checkChannels(channels, currentSettings);

				int nr = PlateUtils.getWellNr(well);

				imageData = imageProvider.render(currentSettings, w, h, nr-1, channels);
				cache.put(key, imageData);
			}
		}
		return imageData;
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
		Object key = getKey(well, scale, channels, region);
		ImageData imageData = (ImageData) cache.get(key);
		if (imageData == null) {
			try (ImageProvider imageProvider = new ImageProvider(well.getPlate())) {
				imageProvider.open();

				ImageSettings currentSettings = ImageSettingsFactory.getSettings(well);
				channels = checkChannels(channels, currentSettings);

				int nr = PlateUtils.getWellNr(well);

				imageData = imageProvider.render(currentSettings, scale, region, nr-1, channels);
				cache.put(key, imageData);
			}
		}
		return imageData;
	}

	public float getImageAspectRatio(Plate plate) {
		// Try using B2 (some plates have empty borders) since this is the closest to 1.
		List<Well> wells = plate.getWells();
		int b2WellNr = plate.getColumns() + 2;
		if (wells.size() > b2WellNr) {
			Well well = PlateUtils.getWell(plate, b2WellNr);
			Point size = getWellImageSize(well, 1f);
			if (size.x != 0 && size.y != 0) return size.x / (float) size.y;
		}

		return getImageAspectRatio(wells);
	}

	public float getImageAspectRatio(List<Well> wells) {
		int currentTry = 0;
		while (currentTry++ < 20) {
			int imageNr = (int) (Math.random() * wells.size());
			Well well = wells.get(imageNr);
			Point size = getWellImageSize(well, 1f);
			if (size.x != 0 && size.y != 0) return size.x / (float) size.y;
		}

		return 1f;
	}

	private Object getKey(Well well, float scale, boolean[] channels, Rectangle region) {
		return new ImageKey(well, scale, channels, region);
	}

	private Object getKey(Well well, float scale, boolean[] channels) {
		return new ImageKey(well, scale, channels);
	}

	private Object getKey(Well well, int w, int h, boolean[] channels) {
		return new ImageKey(well, w, h, channels);
	}

	private boolean[] checkChannels(boolean[] channels, ImageSettings currentSettings) {
		if (channels == null || channels.length != currentSettings.getImageChannels().size()) {
			channels = new boolean[currentSettings.getImageChannels().size()];
			for (int i=0; i<channels.length; i++) {
				channels[i] = currentSettings.getImageChannels().get(i).isShowInPlateView();
			}
		}
		return channels;
	}

}