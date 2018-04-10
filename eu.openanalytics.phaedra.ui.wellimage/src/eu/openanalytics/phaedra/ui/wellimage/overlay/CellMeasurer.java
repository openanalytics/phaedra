package eu.openanalytics.phaedra.ui.wellimage.overlay;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PathData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;

import eu.openanalytics.phaedra.base.util.misc.ImageUtils;
import eu.openanalytics.phaedra.base.util.misc.SWTUtils;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.vo.ImageChannel;
import eu.openanalytics.phaedra.model.protocol.vo.ImageSettings;
import eu.openanalytics.phaedra.wellimage.ImageRenderService;
import eu.openanalytics.phaedra.wellimage.render.ImageRenderRequest;
import eu.openanalytics.phaedra.wellimage.render.ImageRenderRequestFactory;

public class CellMeasurer implements AutoCloseable {
	
	private ImageSettings imageSettings;
	
	public CellMeasurer(Plate plate) throws IOException {
		this.imageSettings = PlateUtils.getProtocolClass(plate).getImageSettings();
	}

	@Override
	public void close() {
		// Nothing to close.
	}
	
	public CellMeasurement measure(Well well, PathData cellPath) throws IOException {
		Rectangle boundingBox = ImageUtils.getBoundingBox(cellPath);
		
		// Inspect all the RAW channels
		int[] channels = new ArrayList<>(imageSettings.getImageChannels()).stream()
				.filter(ch -> ch.getType() == ImageChannel.CHANNEL_TYPE_RAW)
				.mapToInt(ch -> ch.getSequence()).toArray();
		
		CellMeasurement meas = new CellMeasurement(channels.length);
		Point center = SWTUtils.getCenter(boundingBox);
		meas.cogX = center.x;
		meas.cogY = center.y;
		meas.area = SWTUtils.getSurface(cellPath);
		
		ImageRenderRequest req = ImageRenderRequestFactory.forWell(well)
				.withRegion(boundingBox)
				.withApplyBlend(false)
				.withApplyGamma(false)
				.withScale(1.0f)
				.build();
		
		for (int i=0; i<channels.length; i++) {
			int channel = channels[i];
			req.components = new boolean[imageSettings.getImageChannels().size()];
			req.components[channel] = true;
			ImageData img = ImageRenderService.getInstance().getImageData(req);
			
			int[] pixels = ImageUtils.applyBitMask(img, cellPath);
			int totalIntensity = Arrays.stream(pixels).sum();
			int maxIntensity = Arrays.stream(pixels).max().orElse(0);
			double avgIntensity = Arrays.stream(pixels).average().orElse(Double.NaN);
			
			meas.avgIntensities[i] = (float) avgIntensity;
			meas.maxIntensities[i] = (float) maxIntensity;
			meas.totIntensities[i] = (float) totalIntensity;
			
//			System.out.println("Channel " + channel + ", MaxInt: " + maxIntensity + ", AvgInt: " + avgIntensity + ", TotInt: " + totalIntensity);
		}
		
		return meas;
	}
	
	public static class CellMeasurement {
		
		public float cogX;
		public float cogY;
		public float area;
		public float[] avgIntensities;
		public float[] maxIntensities;
		public float[] totIntensities;
		
		public CellMeasurement(int channelCount) {
			avgIntensities = new float[channelCount];
			maxIntensities = new float[channelCount];
			totIntensities = new float[channelCount];
		}
	}
}
