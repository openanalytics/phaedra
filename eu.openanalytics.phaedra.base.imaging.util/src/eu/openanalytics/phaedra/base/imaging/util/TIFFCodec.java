package eu.openanalytics.phaedra.base.imaging.util;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.PaletteData;

import loci.common.services.ServiceFactory;
import loci.formats.CoreMetadata;
import loci.formats.FormatException;
import loci.formats.FormatTools;
import loci.formats.IFormatReader;
import loci.formats.ImageReader;
import loci.formats.MetadataTools;
import loci.formats.in.TiffReader;
import loci.formats.meta.IMetadata;
import loci.formats.out.TiffWriter;
import loci.formats.services.OMEXMLService;
import loci.formats.tiff.IFD;

/**
 * <p>A codec for reading and writing TIFF images, with support for:</p>
 * <ul>
 * <li>8bit or 16bit greyscale images</li>
 * <li>TIFF compression (ZIP, LZW, ...)</li>
 * <li>Multipage TIFF images</li>
 * </ul>
 * This codec uses Bioformats.
 * <p>
 * TODO Rename this codec: it can read many more formats than TIFF
 * </p>
 */
public class TIFFCodec {

	/**
	 * Attempt to read image data from the given file.
	 * 
	 * @param inputFile The file to parse.
	 * @return An array of ImageData. E.g. for a multipage TIFF, the array will contain multiple elements. 
	 * @throws IOException If the file cannot be parsed for any reason.
	 */
	public static ImageData[] read(String inputFile) throws IOException {
		return read(inputFile, (String) null);
	}
	
	public static ImageData[] read(String inputFile, String readerClass) throws IOException {
		IFormatReader reader = getReader(inputFile, readerClass);
		if (reader == null) {
			return new ImageLoader().load(inputFile);
		} else {
			return read(inputFile, reader);
		}
	}
	
	public static ImageData[] read(String inputFile, IFormatReader reader) throws IOException {
		try {
			reader.setGroupFiles(false);
			reader.setId(inputFile);
			
			int w = reader.getSizeX();
			int h = reader.getSizeY();
			int bpp = reader.getBitsPerPixel();
			int bytesPerPixel = Math.max(1, bpp/8);
			int pages = reader.getImageCount();
			
			ImageData[] data = new ImageData[pages];
			for (int page=0; page<pages; page++) {

				// Palette doesn't really matter: we don't use RGB values, but interact with pixel integers directly.
				PaletteData palette = new PaletteData(0xFF, 0xFF, 0xFF);
				data[page] = new ImageData(w, h, bpp, palette);
				
				boolean le = true; // Access endianness only if needed.
				if (bpp > 8) {
					List<CoreMetadata> metadata = reader.getCoreMetadataList();
					if (!metadata.isEmpty()) le = metadata.get(0).littleEndian;
				}
				//TODO Look at the photometric interpretation byte.
				
				byte[] img = reader.openBytes(page);
				int[] pixels = new int[w*h];
				for (int i=0; i<w*h; i++) {
					int index = i*bytesPerPixel;
					if (bpp == 16) {
						// The ImageData pixels are always little endian.
						if (le) pixels[i] = ((img[index+1] & 0xFF) << 8) + (img[index] & 0xFF);
						else pixels[i] = ((img[index] & 0xFF) << 8) + (img[index+1] & 0xFF);
					} else {
						pixels[i] = img[index];
					}
				}
				
				data[page].setPixels(0, 0, w*h, pixels, 0);
			}
			
			return data;
		} catch (FormatException e) {
			throw new IOException("Unsupported image format: " + inputFile, e);
		} finally {
			reader.close();
		}
	}

	public static IFormatReader getReader(String inputFile, String readerClass) throws IOException {
		if (readerClass != null && !readerClass.isEmpty()) {
			try {
				return (IFormatReader) Class.forName(readerClass).newInstance();
			} catch (Exception e) {
				throw new IOException("Failed to load reader class: " + readerClass, e);
			}
		}
		
		// TIFF reader
		String[] tiffExtensions = { ".tif", ".tiff", ".flex" };
		for (String ext: tiffExtensions) {
			if (inputFile.toLowerCase().endsWith(ext)) return new TiffReader();
		}
		
		// null: use SWT ImageLoader instead (faster than Bioformats)
		String[] swtExtensions = { ".bmp", ".gif", ".png", ".jpg", ".jpeg" };
		for (String ext: swtExtensions) {
			if (inputFile.toLowerCase().endsWith(ext)) return null;
		}
		
		// Generic reader
		return new ImageReader();
	}
	
	/**
	 * Write image data to a TIFF file.
	 * 
	 * @param data The image data to write.
	 * @param destination The destination of the TIFF file.
	 * @throws IOException If the image cannot be written for any reason.
	 */
	public static void write(ImageData data, String destination) throws IOException {
		
		File destFile = new File(destination);
		if (destFile.exists()) destFile.delete();
		
		int[] pixels = new int[data.width * data.height];
		data.getPixels(0, 0, pixels.length, pixels, 0);
		
		int bytesPerPixel = Math.max(1, data.depth/8);
		
		byte[] buffer = new byte[pixels.length*bytesPerPixel];
		for (int i=0; i<pixels.length; i++) {
			int pixel = pixels[i];
			if (data.depth == 16) {
				// Write as little endian (see MetadataTools.populateMetadata below).
				buffer[i*2] = (byte)(pixel & 0xFF);
				buffer[i*2+1] = (byte)((pixel >> 8) & 0xFF);
			} else if (data.depth == 1) {
				buffer[i] = (byte)(pixel == 0 ? 0 : 0xFF);
			} else {
				buffer[i] = (byte)(pixel & 0xFF);
			}
		}
		pixels = null;

		try (TiffWriter writer = new TiffWriter()) {
			// Put entire image in a single strip (else Bioformats doesn't calculate the stripByteCounts correctly)
			IFD ifd = new IFD();
			ifd.putIFDValue(IFD.ROWS_PER_STRIP, data.height);
			
			ServiceFactory factory = new ServiceFactory();
			OMEXMLService service = factory.getInstance(OMEXMLService.class);
			IMetadata meta = service.createOMEXMLMetadata();
			int dataType = data.depth == 16 ? FormatTools.UINT16 : FormatTools.UINT8;
			MetadataTools.populateMetadata(meta, 0, null, true, "XYZCT",
					FormatTools.getPixelTypeString(dataType), data.width, data.height, 1, 1, 1, 1);
			
			writer.setCompression("LZW");
			writer.setMetadataRetrieve(meta);
			writer.setId(destination);
			writer.saveBytes(0, buffer, ifd);
		} catch (Exception e) {
			throw new IOException(e);
		}

		if (data.depth == 1) {
			// Not supported by TiffWriter, so do it as a post-processing instead.
			IMConverter.convert(destination, "-depth 1", destination);
		}
	}
}
