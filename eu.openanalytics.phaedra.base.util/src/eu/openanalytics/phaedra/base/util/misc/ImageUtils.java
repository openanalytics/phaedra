package eu.openanalytics.phaedra.base.util.misc;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.ImageTranscoder;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.Path;
import org.eclipse.swt.graphics.PathData;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Transform;
import org.eclipse.swt.widgets.Display;

import eu.openanalytics.phaedra.base.util.convert.AWTImageConverter;

public class ImageUtils {

	/**
	 * Scale an image to another size. The scaling uses interpolation and anti-aliasing to generate
	 * a smooth result.
	 *
	 * The scaled image does not contain an alpha channel.
	 *
	 * @param image The image to scale.
	 * @param w The new image width.
	 * @param h The new image height.
	 * @param disposeOld Dispose image.
	 * @return A scaled image.
	 */
	public static Image scaleByAspectRatio(Image image, int w, int h, boolean disposeOld) {
		if (image == null) return null;
		Image scaled = new Image(Display.getDefault(), w, h);
		GC gc = new GC(scaled);
		gc.setAntialias(SWT.ON);
		gc.setInterpolation(SWT.HIGH);

		int oldWidth = image.getBounds().width;
		int oldHeight = image.getBounds().height;

		// Calculate resize ratios for resizing
		float ratioW = (float) w / (float) oldWidth;
		float ratioH = (float) h / (float) oldHeight;

		// Smaller ratio will ensure that the image fits in the view
		float ratio = ratioW < ratioH ? ratioW : ratioH;

		int newWidth = (int) (oldWidth * ratio);
		int newHeight = (int) (oldHeight * ratio);

		gc.drawImage(image, 0, 0, oldWidth, oldHeight, 0, 0, newWidth, newHeight);
		gc.dispose();
		if (disposeOld)
			image.dispose();

		return scaled;
	}

	/**
	 * Scale an image to another size using a zoom level. The scaling uses interpolation and anti-aliasing
	 * to generate a smooth result.
	 *
	 * @param image The image to scale.
	 * @param zoomLevel The zoom level to scale.
	 * @param disposeOld Dispose image.
	 * @return A scaled image.
	 */
	public static Image zoomByLevel(Image image, int zoomLevel, boolean disposeOld) {
		if (image == null) return null;

		int oldWidth = image.getBounds().width;
		int oldHeight = image.getBounds().height;

		int newWidth = oldWidth * zoomLevel;
		int newHeight = oldHeight * zoomLevel;

		Image scaled = new Image(Display.getDefault(), newWidth, newHeight);
		GC gc = new GC(scaled);
		gc.setAntialias(SWT.ON);
		gc.setInterpolation(SWT.HIGH);

		gc.drawImage(image, 0, 0, oldWidth, oldHeight, 0, 0, newWidth, newHeight);
		gc.dispose();
		if (disposeOld)
			image.dispose();

		return scaled;
	}

	/**
	 * Add transparency to an image by setting alpha to zero for a target color value (e.g. white).
	 * The original image's alpha, if any, will be replaced.
	 *
	 * @param image The image to make transparent.
	 * @param colorToMakeTransparent The color to make transparent.
	 * @return A new image with transparency.
	 */
	public static Image addTransparency(Image image, int colorToMakeTransparent) {
		if (image == null) return null;
		ImageData data = image.getImageData();
		// Create a new alpha channel.
		byte[] alpha = new byte[data.width*data.height];
		for (int y=0;y<data.height;y++) {
			byte[] row = new byte[data.width];
			for (int x=0;x<data.width;x++) {
				if ((data.getPixel(x, y) & 0xFFFFFF) == colorToMakeTransparent) row[x] = (byte)0;
				else row[x] = (byte)255;
			}
			System.arraycopy(row,0,alpha,y*data.width,data.width);
		}
		data.alphaData = alpha;
		image.dispose();
		return new Image(Display.getDefault(), data);
	}

	public static BufferedImage addPadding(BufferedImage image, int paddingX, int paddingY) {
		BufferedImage bi = new BufferedImage(image.getWidth() + 2*paddingX , image.getHeight() + 2*paddingY, image.getType());
		Graphics2D g2d = null;
		try {
			g2d = bi.createGraphics();
			g2d.drawImage(image, null, paddingX, paddingY);
		} finally {
			if (g2d != null) g2d.dispose();
		}
		return bi;
	}

	public static Image addPadding(Image originalImage, int padX, int padY) {
		// Create an empty image, with size = original size + padding.
		Rectangle bounds = originalImage.getBounds();
		ImageData paddedData = new ImageData(bounds.width + 2*padX, bounds.height + 2*padY, 24, new PaletteData(0xFF0000, 0x00FF00, 0x0000FF));

		// Apply transparency where needed.

		// First, set all pixels to transparent.
		for (int x = 0; x < paddedData.width; x++) {
			for (int y = 0; y < paddedData.height; y++) {
				paddedData.setAlpha(x, y, 0);
			}
		}

		// Then, apply alpha values from original image.
		byte[] alphas = new byte[bounds.width * bounds.height];
		originalImage.getImageData().getAlphas(0, 0, alphas.length, alphas, 0);
		int i=0;
		for (int y = 0; y < bounds.height; y++) {
			for (int x = 0; x < bounds.width; x++) {
				int alpha = 0xFF & alphas[i++];
				if (alpha > 0 && alpha < 256) paddedData.setAlpha(x + padX, y + padY, alpha);
			}
		}

		Image paddedImage = new Image(null, paddedData);
		GC gc = new GC(paddedImage);
		gc.drawImage(originalImage, padX, padY);
		gc.dispose();
		originalImage.dispose();

		return paddedImage;
	}

	public static ImageData crop(ImageData img, Rectangle r) {
		ImageData cropped = new ImageData(r.width, r.height, img.depth, img.palette);
		for (int y=0; y<r.height; y++) {
			int[] pixels = new int[r.width];
			img.getPixels(r.x, r.y + y, pixels.length, pixels, 0);
			cropped.setPixels(0, y, pixels.length, pixels, 0);
		}
		return cropped;
	}

	public static ImageData applyGamma(ImageData img, float gamma) {
		ImageData out = new ImageData(img.width, img.height, img.depth, img.palette);

		// Build a gamma LUT
		gamma = 1f / gamma;
		int[] gammaLUT = new int[256];
		for(int i=0; i<gammaLUT.length; i++) {
			gammaLUT[i] = (int) (255 * (Math.pow((double) i / (double) 255, gamma)));
		}

		// Transfer alpha without touching it.
		out.alphaData = img.alphaData;

		// Store the old and new pixel values in arrays.
		int[] pixels = new int[img.width*img.height];
		img.getPixels(0, 0, pixels.length, pixels, 0);
		int[] pixelsOut = new int[pixels.length];

		// Loop over pixels and apply gamma.
		for (int i=0; i<pixels.length; i++) {

			int pix = pixels[i];
			int red = (pix & 0xFF0000) >> 16;
		int green = (pix & 0x00FF00) >> 8;
		int blue = (pix & 0x0000FF);

		red = gammaLUT[red];
		green = gammaLUT[green];
		blue = gammaLUT[blue];

		pixelsOut[i] = (red << 16) + (green << 8) + blue;
		}

		out.setPixels(0, 0, pixelsOut.length, pixelsOut, 0);
		return out;
	}

	public static int blend(int srcValue, int destValue, int alpha) {
		int destR = (destValue & 0xFF0000) >> 16;
		int destG = (destValue & 0x00FF00) >> 8;
		int destB = (destValue & 0x0000FF);
		int srcR = (srcValue & 0xFF0000) >> 16;
		int srcG = (srcValue & 0x00FF00) >> 8;
		int srcB = (srcValue & 0x0000FF);
		int[] newRGB = blend(srcR, srcG, srcB, destR, destG, destB, alpha);
		return (newRGB[0] << 16) + (newRGB[1] << 8) + newRGB[2];
	}

	public static int[] blend(int srcR, int srcG, int srcB, int destR, int destG, int destB, int alpha) {
		int[] out = new int[3];
		float a = alpha / 255f;
		out[0] = (int)(destR * (1-a) + srcR * a);
		out[1] = (int)(destG * (1-a) + srcG * a);
		out[2] = (int)(destB * (1-a) + srcB * a);
		return out;
	}

	/**
	 * Convert the pixels in the given array into a 24bit RGB image without alpha.
	 * The pixels are assumed to represent an 8 bit greyscale image.
	 *
	 * @param pixels The greyscale pixels to convert.
	 * @return An RGB ImageData representing the greyscale image.
	 */
	public static ImageData createRGBImage(int[][] pixels) {
		PaletteData palette = new PaletteData(0xFF0000, 0xFF00, 0xFF);
		ImageData data = new ImageData(pixels.length, pixels[0].length, 24, palette);
		for (int x=0; x<data.width; x++) {
			for (int y=0; y<data.height; y++) {
				int pixelValue = (pixels[x][y] << 16) | (pixels[x][y] << 8) | pixels[x][y];
				data.setPixel(x, y, pixelValue);
			}
		}
		return data;
	}

	/**
	 * Converts a given pixel to an 8 bit pixel.
	 * RGB (24bit) Images are seen as 8 bit images. The red value will be used as the gray value.
	 * @param pixelValue The pixel value to convert to an 8 bit pixel
	 * @param depth The depth of the given pixel
	 * @return An 8 bit pixel
	 */
	public static int to8bit(int pixelValue, int depth) {
		if (depth == 24) {
			depth = 8;
		}
		return to8bit(pixelValue, depth, 0, (int) Math.pow(2, depth) - 1);
	}

	/**
	 * Converts a given pixel to an 8 bit pixel.
	 * Using the min and max variables one can translate a given part of the pixel range.
	 * RGB (24bit) Images are seen as 8 bit images. The red value will be used as the gray value.
	 * @param pixelValue The pixel value to convert to an 8 bit pixel
	 * @param depth The depth of the given pixel
	 * @param min The min value for the LUT
	 * @param max The max value for the LUT
	 * @return An 8 bit pixel
	 */
	public static int to8bit(int pixelValue, int depth, int min, int max) {
		int grayValue;
		if (depth == 16) {
			int leftByte = (pixelValue & 0xFF00) >> 8;
				int rightByte = pixelValue & 0xFF;
				// Grayvalue is an 16bit value
				// Important! We assume the pixel comes from ImageData, which returns 16bit in little endian!
				grayValue = rightByte + 256 * leftByte;
		} else if (depth == 8 || depth == 24) {
			// Grayvalue is an 8bit value
			grayValue = (pixelValue & 0xFF);
		} else if (depth == 1) {
			// GrayValue is a 1bit value
			grayValue = (pixelValue & 0x1);
		} else {
			grayValue = 0;
		}

		// Clip to min-max
		if (grayValue < min) grayValue = min;
		if (grayValue > max) grayValue = max;

		// Scale down to 8bit, within min-max range
		double factor = (grayValue-min) / (double) (max - min);
		grayValue = (int)(factor * 255);

		return grayValue;
	}

	public static int[] applyBitMask(ImageData image, PathData mask) {

		// Render the mask on an image.
		Image maskImg = new Image(null, image.width, image.height);
		GC gc = new GC(maskImg);
		Transform t = new Transform(gc.getDevice());
		Rectangle bounds = getBoundingBox(mask);
		t.translate(-bounds.x, -bounds.y);
		gc.setTransform(t);
		Path path = new Path(null, mask);
		gc.fillPath(path);
		t.dispose();
		gc.dispose();
		path.dispose();

		ImageData maskImgData = maskImg.getImageData();
		maskImg.dispose();

		// Obtain the pixels from the input image and the mask image.
		int[] pixels = new int[image.width * image.height];
		image.getPixels(0, 0, pixels.length, pixels, 0);

		int[] maskPixels = new int[image.width * image.height];
		maskImgData.getPixels(0, 0, maskPixels.length, maskPixels, 0);

		// Retain only pixels that are non-zero in the mask image.
		int[] maskedPixels = new int[pixels.length];
		for (int i=0; i<pixels.length; i++) {
			maskedPixels[i] = (maskPixels[i] == 0) ? 0 : pixels[i];
		}
		return maskedPixels;
	}

	public static Rectangle getBoundingBox(PathData pd) {
		Path path = new Path(null, pd);
		float[] bounds = new float[4];
		path.getBounds(bounds);
		path.dispose();
		int x = (int)bounds[0];
		int y = (int)bounds[1];
		int w = (int)(bounds[2]);
		int h = (int)(bounds[3]);
		Rectangle rect = new Rectangle(x, y, w, h);
		return rect;
	}

	/**
	 * Returns the max colors for a specified bit depth.
	 * @param bitDepth The bit depth of the image (0 is treated as 8)
	 * @return 2^bitDepth - 1
	 */
	public static int getMaxColors(int bitDepth) {
		// We will treat 0 bit depth as 8
		if (bitDepth <= 0) {
			bitDepth = 8;
		}
		return (int) Math.pow(2, bitDepth) - 1;
	}

	/**
	 * Returns the given SVG as Image.
	 * @param data The SVG.
	 * @param w The width of the created Image.
	 * @param h The height of the created Image.
	 * @param bgColor The background color for the Image (Optional).
	 * @return
	 */
	public static Image getSVGAsImage(byte[] data, int w, int h, Color bgColor) {
		BufferedImage img = null;
		try {
			// Transcode the SVG input
			InputStream input = new ByteArrayInputStream(data);
			TranscoderInput transcoderInput = new TranscoderInput(input);

			BufferedImageTranscoder transcoder = new BufferedImageTranscoder();
			transcoder.addTranscodingHint(ImageTranscoder.KEY_WIDTH, Float.valueOf(w));
			transcoder.addTranscodingHint(ImageTranscoder.KEY_HEIGHT, Float.valueOf(h));
			transcoder.transcode(transcoderInput, null);
			img = transcoder.getImage();
		} catch (TranscoderException e) {
			// Will return a blank image.
			img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		}

		Image swtImage = AWTImageConverter.convert(null, img);

		if (bgColor != null) {
			Image tempImage = swtImage;
			swtImage = new Image(null, w, h);
			GC gc = new GC(swtImage);
			gc.setBackground(bgColor);
			gc.fillRectangle(0, 0, w, h);
			gc.drawImage(tempImage, 0, 0);
			gc.dispose();
			tempImage.dispose();
		}

		return swtImage;
	}

	/**
	 * <p>Rotate given {@link ImageData}</p>
	 *
	 * <ul>
	 * 	<li>{@link SWT#LEFT} for 90 degrees left</li>
	 * 	<li>{@link SWT#RIGHT} for 90 degrees right</li>
	 * 	<li>{@link SWT#DOWN} for 180 degrees</li>
	 * </ul>
	 * @param srcData
	 * @param direction
	 * @return
	 */
	public static ImageData rotate(ImageData srcData, int direction) {
		int bytesPerPixel = srcData.bytesPerLine / srcData.width;
		int destBytesPerLine = (direction == SWT.DOWN) ? srcData.width * bytesPerPixel : srcData.height * bytesPerPixel;
		byte[] newData = new byte[srcData.data.length];

		int width = 0, height = 0;
		for (int srcY = 0; srcY < srcData.height; srcY++) {
			for (int srcX = 0; srcX < srcData.width; srcX++) {
				int destX = 0, destY = 0, destIndex = 0, srcIndex = 0;
				switch (direction) {
				case SWT.LEFT: // left 90 degrees
					destX = srcY;
					destY = srcData.width - srcX - 1;
					width = srcData.height;
					height = srcData.width;
					break;
				case SWT.RIGHT: // right 90 degrees
					destX = srcData.height - srcY - 1;
					destY = srcX;
					width = srcData.height;
					height = srcData.width;
					break;
				case SWT.DOWN: // 180 degrees
					destX = srcData.width - srcX - 1;
					destY = srcData.height - srcY - 1;
					width = srcData.width;
					height = srcData.height;
					break;
				}
				destIndex = (destY * destBytesPerLine) + (destX * bytesPerPixel);
				srcIndex = (srcY * srcData.bytesPerLine) + (srcX * bytesPerPixel);
				System.arraycopy(srcData.data, srcIndex, newData, destIndex, bytesPerPixel);
			}
		}

		// destBytesPerLine is used as scanlinePad to ensure that no padding is required
		return new ImageData(width, height, srcData.depth, srcData.palette, destBytesPerLine, newData);
	}

	private static class BufferedImageTranscoder extends ImageTranscoder {
		private BufferedImage image;

		@Override
		public BufferedImage createImage(int w, int h) {
			return new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		}

		@Override
		public void writeImage(BufferedImage awtImage, TranscoderOutput output) throws TranscoderException {
			if (awtImage != null) {
				image = awtImage;
			}
		}

		public BufferedImage getImage() {
			return image;
		}
	}

}