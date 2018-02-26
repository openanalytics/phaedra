package eu.openanalytics.phaedra.base.imaging.jp2k.comp;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;

import eu.openanalytics.phaedra.base.util.misc.ImageUtils;

public class LabelComponent extends BaseComponentType {

	public final static int[] LABEL_COLORS = {
		0xFF0000, 0x7BFF00, 0x00FF9C, 0xFF00BD,
		0x009EFF, 0xFFBE00, 0x7B00FF, 0x00FF39,
		0xFF5D00, 0xDEFF00, 0x1800FF, 0x00FFFF,
		0xDE00FF,};

	@Override
	public String getName() {
		return "Label Overlay";
	}

	@Override
	public int getId() {
		return 3;
	}

	@Override
	public String getDescription() {
		return "A label overlay contains pixel values, each value representing a label.\nEach label gets a distinct color to distinguish it from other labels.";
	}

	@Override
	public void blend(ImageData source, ImageData target, int... params) {
		int[] sourcePixels = new int[target.width*target.height];
		source.getPixels(0, 0, sourcePixels.length, sourcePixels, 0);

		int[] targetPixels = new int[target.width*target.height];
		target.getPixels(0, 0, targetPixels.length, targetPixels, 0);

		int alpha = params[5];
		for (int i=0; i<sourcePixels.length; i++) {
			int overlayValue = sourcePixels[i];
			// Label 0 is reserved for background (i.e. transparent).
			if ((overlayValue & 0xFF) < 1) continue;
			// Skip transparent pixels.
			if (source.alphaData != null && source.alphaData[i] != (byte)255) continue;

			// Scaling 16bit down to 8bit usually results in labels betweeen 0 and 1.
			// So instead, chop off the highest 8 bits.
			if (source.depth == 16) overlayValue = overlayValue & 0xFF;
			else overlayValue = ImageUtils.to8bit(overlayValue, source.depth);

			int index = overlayValue % LABEL_COLORS.length;
			int color = LABEL_COLORS[index];
			if (alpha == 255) targetPixels[i] = color;
			else targetPixels[i] = ImageUtils.blend(color, targetPixels[i], alpha);
		}
		target.setPixels(0, 0, targetPixels.length, targetPixels, 0);
	}

	@Override
	public Image createIcon(Device device) {
		Image img = new Image(device, 20, 20);
		GC gc = new GC(img);

		int sampleColors = 5;
		int heightPerSample = img.getBounds().height/sampleColors;
		for (int i=0; i<sampleColors; i++) {
			int offset = i*heightPerSample;

			int r = (LABEL_COLORS[i] & 0xFF0000) >> 16;
			int g = (LABEL_COLORS[i] & 0x00FF00) >> 8;
			int b = LABEL_COLORS[i] & 0x0000FF;
			Color color = new Color(device,r,g,b);

			gc.setBackground(color);
			gc.fillRectangle(0, offset, 20, heightPerSample);

			color.dispose();
		}

		gc.dispose();
		return img;
	}
}
