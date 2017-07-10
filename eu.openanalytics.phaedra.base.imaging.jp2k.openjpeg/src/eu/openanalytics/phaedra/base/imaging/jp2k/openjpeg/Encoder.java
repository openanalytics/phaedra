package eu.openanalytics.phaedra.base.imaging.jp2k.openjpeg;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.graphics.ImageData;
import org.openjpeg.EncodingParameters;
import org.openjpeg.ImagePixels;
import org.openjpeg.OpenJPEGEncoder;

import eu.openanalytics.phaedra.base.imaging.jp2k.CompressionConfig;
import eu.openanalytics.phaedra.base.imaging.jp2k.IEncodeAPI;
import eu.openanalytics.phaedra.base.imaging.util.TIFFCodec;
import eu.openanalytics.phaedra.base.util.io.FileUtils;
import eu.openanalytics.phaedra.base.util.io.StreamUtils;
import net.java.truevfs.access.TConfig;
import net.java.truevfs.access.TFile;
import net.java.truevfs.access.TFileOutputStream;
import net.java.truevfs.access.TVFS;
import net.java.truevfs.kernel.spec.FsAccessOption;

public class Encoder implements IEncodeAPI {

	@Override
	public void close() {
		// Nothing to close.
	}

	@Override
	public void compressCodeStream(CompressionConfig config, String inputFile, String outputFile) throws IOException {
		ImageData data = TIFFCodec.read(inputFile)[0];
		compressCodeStream(config, data, outputFile);
	}

	@Override
	public void compressCodeStream(CompressionConfig config, ImageData data, String outputFile) throws IOException {
		ImagePixels image = new ImagePixels();
		image.width = data.width;
		image.height = data.height;
		image.depth = data.depth;
		image.pixels = new int[image.width * image.height];
		data.getPixels(0, 0, image.pixels.length, image.pixels, 0);

		EncodingParameters parameters = parseConfig(config);
		validateConfig(image, parameters);
		
		OpenJPEGEncoder encoder = new OpenJPEGEncoder();
		encoder.encode(image, outputFile, parameters);
	}

	@Override
	public void composeCodestreamFile(String[] inputFiles, String outputFile, IProgressMonitor monitor) throws IOException {
		checkOutputFormat(outputFile);
		// Note: do not use DEFLATE, that would break random access inside the codestreams.
		File parent = new File(outputFile).getParentFile();
		parent.mkdirs();
		try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(outputFile))) {
			for (int i = 0; i < inputFiles.length; i++) {
				String fileName = "codestream_" + i + ".j2c";
				File file = new File(inputFiles[i]);
				try (InputStream in = new FileInputStream(file)) {
					ZipEntry zipEntry = new ZipEntry(fileName);
					zipEntry.setTime(file.lastModified());
					zipEntry.setSize(file.length());
					zipEntry.setCrc(StreamUtils.calculateCRC(new FileInputStream(file)));
					zipEntry.setMethod(ZipEntry.STORED);	
					zipOut.putNextEntry(zipEntry);	
					StreamUtils.copy(in, zipOut);
				}
			}
		}
	}

	@Override
	public void updateCodestreamFile(String inputFile, Map<Integer, String> codestreamFiles, String outputFile, IProgressMonitor monitor) throws IOException {
		updateCodestreamFile(new FileInputStream(inputFile), codestreamFiles, outputFile, monitor);
	}
	
	@Override
	public void updateCodestreamFile(InputStream input, Map<Integer, String> codestreamFiles, String outputFile, IProgressMonitor monitor) throws IOException {
		checkOutputFormat(outputFile);
		StreamUtils.copyAndClose(input, new FileOutputStream(outputFile));
		try {
			TConfig config = TConfig.current();
			config.setAccessPreference(FsAccessOption.GROW, true);
			config.setAccessPreference(FsAccessOption.STORE, true);
			for (Integer i: codestreamFiles.keySet()) {
				String file = codestreamFiles.get(i);
				String fileName = "codestream_" + i + ".j2c";
				TFile zipEntry = new TFile(outputFile + "/" + fileName);
				StreamUtils.copyAndClose(new FileInputStream(file), new TFileOutputStream(zipEntry));
			}
		} finally {
			TVFS.umount(new TFile(outputFile));
		}
	}
	
	private static void checkOutputFormat(String outputFile) throws IOException {
		String extension = FileUtils.getExtension(outputFile);
		if (!extension.equalsIgnoreCase("zip")) throw new IOException("Unsupported output format: " + extension);
	}
	
	private static EncodingParameters parseConfig(CompressionConfig config) {
		EncodingParameters parameters = new EncodingParameters();
		parameters.progressionOrder = config.order;
		parameters.resolutionLevels = config.resolutionLevels;

		String[] precincts = config.precincts.split("\\},");
		parameters.precincts = new int[precincts.length * 2];
		for (int i = 0; i < precincts.length; i++) {
			String[] values = precincts[i].split(",");
			parameters.precincts[2*i] = Integer.parseInt(values[0].substring(1));
			if (values[1].endsWith("}")) values[1] = values[1].substring(0, values[1].length()-1);
			parameters.precincts[2*i + 1] = Integer.parseInt(values[1]);
		}
		
		// Required for performant region decoding with OpenJPEG.
		parameters.tileSize = new int[] { 256, 256 };
		
		parameters.psnr = config.psnr;
		
		return parameters;
	}
	
	private static void validateConfig(ImagePixels pixels, EncodingParameters parameters) {
		int size = Math.min(pixels.width, pixels.height);
		
		if (parameters.tileSize != null) {
			size = Math.min(parameters.tileSize[0], parameters.tileSize[1]);
		}
		
		while (size < (1<<parameters.resolutionLevels)) parameters.resolutionLevels--;
	}
}
