package eu.openanalytics.phaedra.base.imaging.jp2k;

import java.io.IOException;

import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;

import eu.openanalytics.phaedra.base.imaging.jp2k.codestream.CodeStreamAccessor;

/**
 * This API is used for decoding multiple images that are organized in a single source.
 * Individual images are accessed via two numbers, an image number (zero-based) and a component number (zero-based).
 * 
 * For example, if each image has 4 components, codestream 22 can be accessed by using imageNr 5 codestream 2.
 * For Phaedra well images, imageNr = wellNr-1 and componentNr = channelIndex. 
 */
public interface IDecodeAPI extends AutoCloseable {

	/**
	 * Open the source(s) containing the JPEG2000 data that will be decoded.
	 * 
	 * @throws IOException If the source(s) cannot be opened.
	 */
	public void open() throws IOException;
	
	/**
	 * Close the source(s) that were opened by {@link open}.
	 */
	public void close();
	
	/**
	 * Change the background color to use in renderings
	 * (the area of renders not actually occuppied by pixels from the JPEG2000 image).
	 * 
	 * @param bgColor The new background color.
	 */
	public void setBgColor(int bgColor);
	
	/**
	 * Get the 2-dimensional (x,y) size of the specified image.
	 * 
	 * @param imageNr The image number, starting from zero.
	 * @return The original image size, in pixels.
	 * @throws IOException If the image could not be accessed.
	 */
	public Point getSize(CodeStreamAccessor accessor) throws IOException;
	
	/**
	 * Get the bit depth of the specified component in the specified image.
	 * 
	 * @param imageNr The image number, starting from zero.
	 * @param component The component number, starting from zero.
	 * @return The bit depth of the codestream.
	 * @throws IOException If the image could not be accessed.
	 */
	public int getBitDepth(CodeStreamAccessor accessor) throws IOException;
	
	/**
	 * Render an individual image component.
	 * 
	 * Note: this method is not suited for rendering overlays due to scaling
	 * artifacts. For overlays, use {@link renderImage(float, int, int)} instead.
	 * 
	 * @param w The width of the image to render to. Pass 0 to use original width.
	 * @param h The height of the image to render to. Pass 0 to use original height.
	 * @param imageNr The number of the image to render, starting from zero.
	 * @param component The component number to render, starting from zero.
	 * @return The pixels of the rendered image.
	 * @throws IOException If the rendering failed.
	 */
	public ImageData renderImage(int w, int h, CodeStreamAccessor accessor) throws IOException;

	public ImageData renderImage(float scale, CodeStreamAccessor accessor) throws IOException;
	
	public ImageData renderImage(float scale, int additionalDiscardLevels, CodeStreamAccessor accessor) throws IOException;
	
	/**
	 * Render a region of an image.
	 * The region coordinates (in pixels) are assumed to be in the original image space (i.e. with scale = 1.0f).
	 * 
	 * When using scale factors that are not powers of 2, the image (especially overlays)
	 * may contain artifacts caused by pixel interpolation.
	 * 
	 * @param scale The scale factor to use.
	 * @param region The coordinates of the region in the image to render.
	 * @param imageNr The number of the image to render from, starting from zero.
	 * @param component The component number to render, starting from zero.
	 * @return The pixels of the rendered image.
	 * @throws IOException If the rendering failed.
	 */
	public ImageData renderImageRegion(float scale, Rectangle region, CodeStreamAccessor accessor) throws IOException;

	public ImageData renderImageRegion(float scale, int additionalDiscardLevels, Rectangle region, CodeStreamAccessor accessor) throws IOException;
	
}
