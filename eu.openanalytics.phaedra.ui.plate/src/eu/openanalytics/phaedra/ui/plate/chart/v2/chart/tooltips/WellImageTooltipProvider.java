package eu.openanalytics.phaedra.ui.plate.chart.v2.chart.tooltips;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;

import eu.openanalytics.phaedra.base.ui.charting.v2.chart.TooltipsSettings;
import eu.openanalytics.phaedra.base.ui.charting.v2.data.IDataProvider;
import eu.openanalytics.phaedra.base.util.convert.AWTImageConverter;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.vo.ImageChannel;
import eu.openanalytics.phaedra.model.protocol.vo.ImageSettings;
import eu.openanalytics.phaedra.wellimage.ImageRenderService;
import uk.ac.starlink.ttools.plot.Tooltip;
import uk.ac.starlink.ttools.plot.TooltipsPlot.ITooltipProvider;

public class WellImageTooltipProvider implements ITooltipProvider {

	public static final String CFG_SCALE = "scale";
	public static final String CFG_CHANNELS = "channels";

	public static final float DEFAULT_SCALE = 1/32f;
	public static final boolean[] DEFAULT_CHANNELS = null;

	private IDataProvider<Plate, Well> dataProvider;

	private float scale;
	private boolean[] channels;

	public WellImageTooltipProvider(IDataProvider<Plate, Well> dataProvider) {
		this.dataProvider = dataProvider;

		this.scale = DEFAULT_SCALE;
		this.channels = DEFAULT_CHANNELS;
	}

	@Override
	public void starting() {
		// Do nothing.
	}

	@Override
	public Point getTooltipSize(int index) {
		Object rowObject = dataProvider.getRowObject(index);
		if (rowObject == null) return null;
		Well w = SelectionUtils.getAsClass(rowObject, Well.class);
		org.eclipse.swt.graphics.Point size = ImageRenderService.getInstance().getWellImageSize(w, scale);
		return new Point(size.x, size.y);
	}

	@Override
	public Tooltip getTooltip(int index) throws IOException {
		Well w = SelectionUtils.getAsClass(dataProvider.getRowObject(index), Well.class);

		ImageSettings currentSettings = PlateUtils.getProtocolClass(w).getImageSettings();

		List<ImageChannel> imageChannels = currentSettings.getImageChannels();
		if (channels == null || imageChannels.size() != channels.length) {
			channels = new boolean[imageChannels.size()];
			for (int i = 0; i < imageChannels.size(); i++) {
				channels[i] = imageChannels.get(i).isShowInWellView();
			}
		}

		ImageData imageData = ImageRenderService.getInstance().getWellImageData(w, scale, channels);
		Image tmpImage = new Image(null, imageData);
		try {
			BufferedImage awtImage = AWTImageConverter.convert(tmpImage);
			return new Tooltip(awtImage, "Well " + PlateUtils.getWellCoordinate(w));
		} finally {
			tmpImage.dispose();
		}
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

}