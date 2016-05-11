package eu.openanalytics.phaedra.base.util.convert;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBufferInt;
import java.awt.image.DirectColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

public class AWTImageConverter {

	/**
	 * Convert an AWT BufferedImage to an SWT Image.
	 *
	 * @param display The Display device to create the Image for.
	 * @param awtImage The AWT Image to convert.
	 * @return An SWT image, ready for use.
	 */
	public static Image convert(Display display, java.awt.Image awtImage) {
		Image swtImage = new Image(display, convert(awtImage));
		return swtImage;
	}

	public static ImageData convert(java.awt.Image awtImage) {

		int width = awtImage.getWidth(null);
		int height = awtImage.getHeight(null);

		// Convert to a BufferedImage if needed, for direct pixel buffer access.
		BufferedImage bufferedImage = null;
		if (awtImage instanceof BufferedImage) {
			bufferedImage = (BufferedImage)awtImage;
		} else {
			bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g2d = bufferedImage.createGraphics();
			g2d.drawImage(awtImage, 0, 0, null);
			g2d.dispose();
		}

		// Transfer the RGB values.
		int[] data = ((DataBufferInt)bufferedImage.getData().getDataBuffer()).getData();
		ImageData imageData = new ImageData(width, height, 24,
				new PaletteData(0xFF0000, 0x00FF00, 0x0000FF));
		imageData.setPixels(0, 0, data.length, data, 0);

		// If alpha is present, transfer that as well.
		if (bufferedImage.getColorModel().hasAlpha()) {
			int[] alpha = ((DataBufferInt)bufferedImage.getAlphaRaster().getDataBuffer()).getData();
			byte[] alphaBytes = new byte[alpha.length];
			for (int i=0; i<alpha.length; i++) {
				alphaBytes[i] = (byte)((alpha[i] >> 24) & 0xFF);
			}
			imageData.setAlphas(0, 0, alphaBytes.length, alphaBytes, 0);
		}

		return imageData;
	}

	public static BufferedImage convert(Image swtImage) {

		ImageData data = swtImage.getImageData();
		return convert(data);
	}

	public static BufferedImage convert(ImageData imageData) {
		ColorModel colorModel = null;
		PaletteData palette = imageData.palette;
		if (palette.isDirect) {
			colorModel = new DirectColorModel(imageData.depth, palette.redMask,
					palette.greenMask, palette.blueMask);
			BufferedImage bufferedImage = new BufferedImage(colorModel,
					colorModel.createCompatibleWritableRaster(imageData.width,
							imageData.height), false, null);
			WritableRaster raster = bufferedImage.getRaster();
			int[] pixelArray = new int[3];
			for (int y = 0; y < imageData.height; y++) {
				for (int x = 0; x < imageData.width; x++) {
					int pixel = imageData.getPixel(x, y);
					RGB rgb = palette.getRGB(pixel);
					pixelArray[0] = rgb.red;
					pixelArray[1] = rgb.green;
					pixelArray[2] = rgb.blue;
					raster.setPixels(x, y, 1, 1, pixelArray);
				}
			}
			return bufferedImage;
		} else {
			RGB[] rgbs = palette.getRGBs();
			byte[] red = new byte[rgbs.length];
			byte[] green = new byte[rgbs.length];
			byte[] blue = new byte[rgbs.length];
			for (int i = 0; i < rgbs.length; i++) {
				RGB rgb = rgbs[i];
				red[i] = (byte) rgb.red;
				green[i] = (byte) rgb.green;
				blue[i] = (byte) rgb.blue;
			}
			if (imageData.transparentPixel != -1) {
				colorModel = new IndexColorModel(imageData.depth, rgbs.length, red,
						green, blue, imageData.transparentPixel);
			} else {
				colorModel = new IndexColorModel(imageData.depth, rgbs.length, red,
						green, blue);
			}
			BufferedImage bufferedImage = new BufferedImage(colorModel,
					colorModel.createCompatibleWritableRaster(imageData.width,
							imageData.height), false, null);
			WritableRaster raster = bufferedImage.getRaster();
			int[] pixelArray = new int[1];
			for (int y = 0; y < imageData.height; y++) {
				for (int x = 0; x < imageData.width; x++) {
					int pixel = imageData.getPixel(x, y);
					pixelArray[0] = pixel;
					raster.setPixel(x, y, pixelArray);
				}
			}
			return bufferedImage;
		}
	}

}
