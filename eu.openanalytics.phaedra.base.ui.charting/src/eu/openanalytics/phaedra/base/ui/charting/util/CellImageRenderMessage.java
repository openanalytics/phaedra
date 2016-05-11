package eu.openanalytics.phaedra.base.ui.charting.util;

import java.awt.image.BufferedImage;

import org.eclipse.swt.graphics.ImageData;

import eu.openanalytics.phaedra.base.ui.gridviewer.concurrent.ConcurrentTaskResult;
import eu.openanalytics.phaedra.base.util.convert.AWTImageConverter;

/**
 * Utility class to convert an AWT BufferedImage object to an SWT ImageData object.
 */
public class CellImageRenderMessage extends ConcurrentTaskResult {

	public CellImageRenderMessage(int row, int column, BufferedImage bi) {
		super(row, column, convert(bi));
	}

	private static ImageData convert(BufferedImage image) {
		int w = image.getWidth();
		int h = image.getHeight();
		int type = image.getType();
		
		int[] data = new int[w*h];
		image.getRGB(0, 0, w, h, data, 0, w);
		
		BufferedImage bi = new BufferedImage(w, h, type);
		bi.setRGB(0, 0, w, h, data, 0, w);
		return AWTImageConverter.convert(bi);
	}
}
