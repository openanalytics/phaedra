package eu.openanalytics.phaedra.ui.subwell.chart.v2.chart.tooltips;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Rectangle;

import eu.openanalytics.phaedra.base.ui.charting.v2.chart.TooltipsSettings;
import eu.openanalytics.phaedra.base.ui.charting.v2.data.IDataProvider;
import eu.openanalytics.phaedra.base.util.convert.AWTImageConverter;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.wellimage.ImageRenderService;
import uk.ac.starlink.ttools.plot.Tooltip;
import uk.ac.starlink.ttools.plot.TooltipsPlot.ITooltipProvider;

public class SubWellImageTooltipProvider implements ITooltipProvider {

	public static final String CFG_SCALE = "scale";
	public static final String CFG_CHANNELS = "channels";

	public static final float DEFAULT_SCALE = 1f;
	public static final boolean[] DEFAULT_CHANNELS = null;

	private IDataProvider<Well, Well> dataProvider;

	private float scale;
	private boolean[] channels;

	public SubWellImageTooltipProvider(IDataProvider<Well, Well> dataProvider) {
		this.dataProvider = dataProvider;

		this.scale = DEFAULT_SCALE;
		this.channels = DEFAULT_CHANNELS;
	}

	@Override
	public void starting() {
		// Do nothing.
	}

	@Override
	public Point getTooltipSize(int subwellIndex) {
		Well currentWell = dataProvider.getKey(subwellIndex);
		if (currentWell == null) return null;
		int[] range = dataProvider.getKeyRange(currentWell);
		int offsetInWell = subwellIndex - range[0];
		Rectangle rect = getTooltipBounds(currentWell, offsetInWell);
		return new Point(rect.width, rect.height);
	}

	@Override
	public Tooltip getTooltip(int subwellIndex) throws IOException {
		Well currentWell = dataProvider.getKey(subwellIndex);
		if (currentWell == null) return null;

		int[] range = dataProvider.getKeyRange(currentWell);
		int offsetInWell = subwellIndex - range[0];

		if (dataProvider.getCurrentFilter().get(subwellIndex)) {
			int wellNr = PlateUtils.getWellNr(currentWell);
			String label = "Cell " + offsetInWell + " @ Well " + PlateUtils.getWellCoordinate(currentWell);
			BufferedImage image = getSubWellImage(currentWell, wellNr, offsetInWell);
			return new Tooltip(image, label);
		}
		return null;
	}

	@Override
	public void setConfig(Object config) {
		if (config instanceof TooltipsSettings) {
			Object obj = ((TooltipsSettings) config).getMiscSetting(CFG_SCALE);
			if (obj instanceof Float) {
				this.scale = (float) obj;
			} else {
				this.scale = DEFAULT_SCALE;
			}
			obj = ((TooltipsSettings) config).getMiscSetting(CFG_CHANNELS);
			if (obj instanceof boolean[]) {
				boolean[] newChannels = (boolean[]) obj;
				this.channels = Arrays.copyOf(newChannels, newChannels.length);
			} else {
				this.channels = DEFAULT_CHANNELS;
			}
		}
	}

	@Override
	public void dispose() {
		// Do nothing.
	}

	private BufferedImage getSubWellImage(Well well, int wellNr, int subwellIndex) throws IOException {
		ImageData imageData = ImageRenderService.getInstance().getSubWellImageData(well, subwellIndex, scale, channels);

		Image tmpImage = new Image(null, imageData);
		try {
			BufferedImage awtImage = AWTImageConverter.convert(tmpImage);
			return awtImage;
		} finally {
			tmpImage.dispose();
		}
	}

	private Rectangle getTooltipBounds(Well well, int subwellIndex) {
		return ImageRenderService.getInstance().getSubWellImageBounds(well, subwellIndex, scale);
	}

}