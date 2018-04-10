package eu.openanalytics.phaedra.wellimage.render;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.eclipse.swt.graphics.ImageData;

import eu.openanalytics.phaedra.base.imaging.jp2k.IDecodeAPI;
import eu.openanalytics.phaedra.base.util.misc.ImageUtils;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.protocol.vo.ImageChannel;
import eu.openanalytics.phaedra.model.protocol.vo.ImageSettings;
import eu.openanalytics.phaedra.wellimage.ImageSettingsFactory;
import eu.openanalytics.phaedra.wellimage.component.ComponentBlender;

public class ImageRenderer {

	public ImageData render(ImageRenderRequest req, IDecodeAPI api) throws IOException {

		ImageSettings imageSettings = req.customSettings;
		if (imageSettings == null) imageSettings = ImageSettingsFactory.getSettings(req.well);
		List<ImageChannel> channels = imageSettings.getImageChannels();
		
		// Determine which components should be rendered.
		boolean[] compFilter = req.components;
		if (compFilter == null || compFilter.length == 0) {
			// If no component filter is provided, assume that all components must be rendered.
			compFilter = new boolean[channels.size()];
			Arrays.fill(compFilter, true);
		}
		if (compFilter.length > channels.size()) {
			compFilter = Arrays.copyOf(compFilter, channels.size());
		}

		int enabledCompCnt = 0;
		for (boolean enabled : compFilter) {
			if (enabled) enabledCompCnt++;
		}

		int imageNr = PlateUtils.getWellNr(req.well) - 1;
		
		ImageData[] datas = new ImageData[enabledCompCnt];
		ImageChannel[] enabledChannels = new ImageChannel[enabledCompCnt];

		// Render all enabled components sequentially.
		int index = 0;
		for (int compNr = 0; compNr < compFilter.length; compNr++) {
			if (!compFilter[compNr]) continue;

			if (req.region != null) {
				datas[index] = api.renderImageRegion(req.scale, req.region, imageNr, compNr);
			} else if (req.size != null) {
				datas[index] = api.renderImage(req.size.x, req.size.y, imageNr, compNr);
			} else if (req.scale != 0.0f) {
				datas[index] = api.renderImage(req.scale, imageNr, compNr);
			} else {
				throw new IOException("Cannot render: no render scale or size provided");
			}
			
			enabledChannels[index++] = channels.get(compNr);
		}
		
		// Blend the components into a single output image.
		ImageData data = datas[0];
		if (req.applyBlend) {
			ComponentBlender blender = new ComponentBlender(enabledChannels);
			data = blender.blend(datas);
		}

		// Apply gamma, but only if gamma != 1.0
		if (req.applyGamma) {
			float gamma = ((float)imageSettings.getGamma())/10;
			if (data != null && gamma != 1f) data = ImageUtils.applyGamma(data, gamma);
		}

		return data;
	}

}
