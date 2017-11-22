package eu.openanalytics.phaedra.base.imaging.util;

import java.io.IOException;

import eu.openanalytics.phaedra.base.util.io.FileUtils;
import loci.formats.FormatException;
import loci.formats.IFormatReader;
import loci.formats.ImageReader;
import loci.formats.in.TiffReader;

/**
 * Use one of the available image codecs to identify an image file.
 */
public class ImageIdentifier {

	/**
	 * Identify the size of an image.
	 * 
	 * @param input The image file to identiy.
	 * @return An array with 4 values: 0 = width, 1 = height, 2 = not used, 3 = bit depth
	 * @throws IOException If the image cannot be identified for any reason.
	 */
	public static int[] identify(String input) throws IOException {
		int[] size = new int[4];
		IFormatReader reader = null;
		String extension = FileUtils.getExtension(input).toLowerCase();
		// Otherwise Bioformats uses some weird MIASReader that fails with a NumberFormatException.
		if (extension.equals("tif") || extension.equals("tiff")) reader = new TiffReader();
		else reader = new ImageReader();
		
		try {
			reader.setGroupFiles(false);
			reader.setId(input);
			size[0] = reader.getSizeX();
			size[1] = reader.getSizeY();
			size[3] = reader.getBitsPerPixel();
		} catch (FormatException e) {
			throw new IOException("Unsupported image format: " + input, e);
		} finally {
			reader.close();
		}
		return size;
	}
}
