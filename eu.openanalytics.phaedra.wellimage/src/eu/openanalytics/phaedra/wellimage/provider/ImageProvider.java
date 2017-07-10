package eu.openanalytics.phaedra.wellimage.provider;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.util.Arrays;
import java.util.List;

import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;

import eu.openanalytics.phaedra.base.environment.Screening;
import eu.openanalytics.phaedra.base.hdf5.HDF5File;
import eu.openanalytics.phaedra.base.imaging.jp2k.CodecFactory;
import eu.openanalytics.phaedra.base.imaging.jp2k.IDecodeAPI;
import eu.openanalytics.phaedra.base.util.misc.ImageUtils;
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.protocol.vo.ImageChannel;
import eu.openanalytics.phaedra.model.protocol.vo.ImageSettings;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.wellimage.ImageSettingsFactory;
import eu.openanalytics.phaedra.wellimage.component.ComponentBlender;

/**
 * <p>This class provides access to a plate's well images.
 * The image can come from a JP2K file or a HDF5 file, depending on the protocol class configuration.</p>
 * <p>Before images can be rendered, the open() method must be called first. When the provider is no longer
 * needed, the close() method must be called.</p>
 */
public class ImageProvider implements AutoCloseable {

	private Plate plate;
	private IDecodeAPI imageFile;
	private HDF5File hdf5File;
	private boolean open;

	public ImageProvider(Plate plate) {
		this.plate = plate;
	}

	public void open() throws IOException {
		if (open) return;
		open = true;

		// Check the channels: no need to open a file if it is not going to be used.
		ProtocolClass pClass = getProtocolClass();
		List<ImageChannel> channels = pClass.getImageSettings().getImageChannels();
		if (channels == null || channels.isEmpty()) return;
		boolean hasJPEG2000Channel = false;
		boolean hasHDF5Channel = false;
		for (ImageChannel channel: channels) {
			if (channel.getSource() == 0 || channel.getSource() == ImageChannel.CHANNEL_SOURCE_JP2K) hasJPEG2000Channel = true;
			if (channel.getSource() == ImageChannel.CHANNEL_SOURCE_HDF5) hasHDF5Channel = true;
		}

		if (hasJPEG2000Channel) {
			String imagePath = PlateService.getInstance().getImageFSPath(plate);
			SeekableByteChannel channel = Screening.getEnvironment().getFileServer().getChannel(imagePath, "r");
			
			if (channel != null) imageFile = CodecFactory.getDecoder(channel, PlateUtils.getWellCount(plate), channels.size());
			if (imageFile != null) imageFile.open();
		}

		String hdf5Path = PlateService.getInstance().getPlateFSPath(plate) + "/" + plate.getId() + ".h5";
		if (hasHDF5Channel && Screening.getEnvironment().getFileServer().exists(hdf5Path)) {
			hdf5File = HDF5File.openForRead(hdf5Path);
		}
	}

	public ProtocolClass getProtocolClass() {
		return PlateUtils.getProtocolClass(plate);
	}

	public IDecodeAPI getDecoder() {
		return imageFile;
	}
	
	@Override
	public void close() {
		if (imageFile != null) imageFile.close();
		if (hdf5File != null) hdf5File.close();
		imageFile = null;
		hdf5File = null;
		open = false;
	}

	public Point getImageSize(int imageNr) throws IOException {
		checkOpen();
		//TODO This assumes that there is at least one image channel.
		if (imageFile != null) return imageFile.getSize(imageNr);
		return new Point(0,0);
	}

	public ImageData render(ImageSettings imSettings, int w, int h, int imageNr, boolean[] enabledComponents) throws IOException {
		checkOpen();
		ImageRenderSettings renderSettings = new ImageRenderSettings();
		renderSettings.width = w;
		renderSettings.height = h;
		return doRender(imSettings, imageNr, enabledComponents, renderSettings);
	}

	public ImageData render(ImageSettings imSettings, float scale, int imageNr, boolean[] enabledComponents) throws IOException {
		checkOpen();
		ImageRenderSettings renderSettings = new ImageRenderSettings();
		renderSettings.scale = scale;
		return doRender(imSettings, imageNr, enabledComponents, renderSettings);
	}

	public ImageData render(ImageSettings imSettings, float scale, int additionalDiscardLevels
			, int imageNr, boolean[] enabledComponents) throws IOException {

		checkOpen();
		ImageRenderSettings renderSettings = new ImageRenderSettings();
		renderSettings.scale = scale;
		renderSettings.additionalDiscardLevels = additionalDiscardLevels;
		return doRender(imSettings, imageNr, enabledComponents, renderSettings);
	}

	public ImageData render(ImageSettings imSettings, float scale, Rectangle region, int imageNr, boolean[] enabledComponents) throws IOException {
		checkOpen();
		ImageRenderSettings settings = new ImageRenderSettings();
		settings.scale = scale;
		settings.region = region;
		return doRender(imSettings, imageNr, enabledComponents, settings);
	}

	public ImageData render(ImageSettings imSettings, float scale, int additionalDiscardLevels, Rectangle region, int imageNr, boolean[] enabledComponents) throws IOException {
		checkOpen();
		ImageRenderSettings settings = new ImageRenderSettings();
		settings.scale = scale;
		settings.region = region;
		settings.additionalDiscardLevels = additionalDiscardLevels;
		return doRender(imSettings, imageNr, enabledComponents, settings);
	}

	public ImageData render(ImageSettings imSettings, ImageRenderSettings renderSettings, int imageNr, boolean[] enabledComponents) throws IOException {
		checkOpen();
		return doRender(imSettings, imageNr, enabledComponents, renderSettings);
	}

	/**
	 * Convencience render function.
	 * Uses the active image settings from ImageSettingsService.
	 *
	 * @param w The width of the image to generate.
	 * @param h The height of the image to generate.
	 * @param imageNr The image nr (well nr - 1) to render.
	 * @return The data of a rendered image.
	 * @throws IOException If an I/O error occurs during rendering.
	 */
	public ImageData render(int w, int h, int imageNr) throws IOException {
		checkOpen();

		ImageSettings settings = ImageSettingsFactory.getSettings(plate);
		boolean[] enabledChannels = new boolean[settings.getImageChannels().size()];
		boolean allDisabled = true;
		for (int i = 0; i < enabledChannels.length; i++) {
			enabledChannels[i] = settings.getImageChannels().get(i).isShowInPlateView();
			if (enabledChannels[i]) allDisabled = false;
		}
		if (allDisabled && enabledChannels.length > 0) enabledChannels[0] = true;
		return render(settings, w, h, imageNr, enabledChannels);
	}

	public ImageData render(int w, int h, int imageNr, boolean[] enabledChannels) throws IOException {
		checkOpen();

		ImageSettings settings = ImageSettingsFactory.getSettings(plate);
		boolean allDisabled = true;
		for (int i = 0; i < enabledChannels.length; i++) {
			if (enabledChannels[i]) allDisabled = false;
		}
		if (allDisabled && enabledChannels.length > 0) enabledChannels[0] = true;
		return render(settings, w, h, imageNr, enabledChannels);
	}
	
	public ImageData render(Rectangle region, int imageNr) throws IOException {
		checkOpen();

		ImageSettings settings = ImageSettingsFactory.getSettings(plate);
		boolean[] enabledChannels = new boolean[settings.getImageChannels().size()];
		boolean allDisabled = true;
		for (int i = 0; i < enabledChannels.length; i++) {
			enabledChannels[i] = settings.getImageChannels().get(i).isShowInPlateView();
			if (enabledChannels[i]) allDisabled = false;
		}
		if (allDisabled && enabledChannels.length > 0) enabledChannels[0] = true;
		return render(settings, 1.0f, region, imageNr, enabledChannels);
	}

	/*
	 * Non-public
	 * **********
	 */

	private void checkOpen() throws IOException {
		if (!open) throw new IOException("ImageProvider is not open");
	}

	private ImageData doRender(ImageSettings imSettings, int imageNr, boolean[] enabledComponents, ImageRenderSettings renderSettings) throws IOException {

		List<ImageChannel> channels = imSettings.getImageChannels();
		if (enabledComponents == null) {
			// If not provided, assume that all components must be rendered.
			enabledComponents = new boolean[channels.size()];
			Arrays.fill(enabledComponents, true);
		}
		if (channels.size() != enabledComponents.length) {
			throw new RuntimeException("The argument 'enabledComponents' must have length equal to the number of image channels");
		}

		int enabledCompCnt = 0;
		for (boolean enabled : enabledComponents) {
			if (enabled) enabledCompCnt++;
		}

		ImageData[] datas = new ImageData[enabledCompCnt];
		ImageChannel[] enabledChannels = new ImageChannel[enabledCompCnt];
		int index = 0;
		for (int compNr=0; compNr<channels.size(); compNr++) {
			if (!enabledComponents[compNr]) continue;

			ImageDataProvider provider = null;
			int src = channels.get(compNr).getSource();
			if (src == ImageChannel.CHANNEL_SOURCE_HDF5) provider = new HDF5ImageProvider();
			else provider = new JPEG2000ImageProvider();

			datas[index] = provider.createImage(this, imageNr, compNr, renderSettings);
			enabledChannels[index++] = channels.get(compNr);
		}
		ImageData data = null;
		if (renderSettings.blend) {
			ComponentBlender blender = new ComponentBlender(enabledChannels);
			data = blender.blend(datas);
		} else {
			data = datas[0];
		}

		if (renderSettings.applyGamma) {
			// Apply gamma, but only if gamma != 1.0
			float gamma = ((float)imSettings.getGamma())/10;
			if (data != null && gamma != 1f) data = ImageUtils.applyGamma(data, gamma);
		}

		return data;
	}

	/*
	 * Different image generators based on arguments.
	 */

	public static class ImageRenderSettings {
		public int width = -1;
		public int height = -1;
		public int additionalDiscardLevels = 0;
		public float scale = 0f;
		public Rectangle region = null;
		public boolean blend = true;
		public boolean applyGamma = true;
	}

	private static interface ImageDataProvider {
		public ImageData createImage(ImageProvider generator, int imageNr, int compNr, ImageRenderSettings renderSettings) throws IOException;
	}

	private static class JPEG2000ImageProvider implements ImageDataProvider {
		@Override
		public ImageData createImage(ImageProvider generator, int imageNr, int compNr, ImageRenderSettings renderSettings) throws IOException {
			if (generator.imageFile == null) return null;
			if (renderSettings.region != null) {
				return generator.imageFile.renderImageRegion(renderSettings.scale, renderSettings.additionalDiscardLevels
						, renderSettings.region, imageNr, compNr);
			}
			if (renderSettings.scale != 0f) {
				return generator.imageFile.renderImage(renderSettings.scale, renderSettings.additionalDiscardLevels, imageNr, compNr);
			}
			if (renderSettings.width != -1 && renderSettings.height != -1) {
				return generator.imageFile.renderImage(renderSettings.width, renderSettings.height, imageNr, compNr);
			}
			return null;
		}
	}

	private static class HDF5ImageProvider implements ImageDataProvider {
		@Override
		public ImageData createImage(ImageProvider generator, int imageNr, int compNr, ImageRenderSettings renderSettings) throws IOException {
			ImageData data = null;
			int[][] pixels = null;

			String imagePath = (HDF5File.getImageDataPath() + "/" + (imageNr+1));
			Point imageSize = generator.getImageSize(imageNr);
			boolean imageAvailable = generator.hdf5File != null && generator.hdf5File.exists(imagePath);

			if (renderSettings.region != null) {
				if (imageAvailable) pixels = getImage(generator.hdf5File, imagePath, renderSettings.region);
				else pixels = new int[imageSize.x][imageSize.y];
				data = scale(ImageUtils.createRGBImage(pixels), renderSettings.scale);
			} else if (renderSettings.scale != 0f) {
				if (imageAvailable) pixels = getImage(generator.hdf5File, imagePath);
				else pixels = new int[imageSize.x][imageSize.y];
				data = scale(ImageUtils.createRGBImage(pixels), renderSettings.scale);
			} else {
				//TODO Disabled for now: plate grids showing a HDF5 channel crash the application.
				//					if (imageAvailable) pixels = getImage(generator.hdf5File, imagePath);
				pixels = new int[imageSize.x][imageSize.y];
				data = scale(ImageUtils.createRGBImage(pixels), renderSettings.width, renderSettings.height);
			}
			return data;
		}

		private int[][] getImage(HDF5File file, String path, Rectangle region) throws IOException {
			return file.getImageSlice(path,
					new long[]{0, 0, region.x, region.y},
					new long[]{1, 1, region.width, region.height},
					null);
		}

		private int[][] getImage(HDF5File file, String path) throws IOException {
			return file.getImageSlice(path,
					new long[]{0, 0, 0, 0},
					new long[]{1, 1, -1, -1},
					null);
		}

		private ImageData scale(ImageData data, float scale) {
			int w = (int)(data.width * scale);
			int h = (int)(data.height * scale);
			return scale(data, w, h);
		}

		private ImageData scale(ImageData data, int w, int h) {
			if (w == data.width && h == data.height) return data;
			//TODO This type of scaling gives ugly results when zooming out.
			return data.scaledTo(w, h);
		}
	}
}
