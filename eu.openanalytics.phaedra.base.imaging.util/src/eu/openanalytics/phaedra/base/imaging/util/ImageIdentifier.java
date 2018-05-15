package eu.openanalytics.phaedra.base.imaging.util;

import java.io.IOException;

import loci.formats.FormatException;
import loci.formats.IFormatReader;
import loci.formats.ImageReader;

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
		
		IFormatReader reader = TIFFCodec.getReader(input);
		if (reader == null) reader = new ImageReader();
		
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
