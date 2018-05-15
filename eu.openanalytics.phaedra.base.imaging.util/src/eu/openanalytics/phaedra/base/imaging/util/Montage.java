package eu.openanalytics.phaedra.base.imaging.util;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;

public class Montage {

	public static void montage(String[] inputFiles, String layout, int padding, String outputFile) throws IOException {
		// Note: layout must be zero-based!
		
		// Analyze the layout (or use the default layout).
		int imageCount = inputFiles.length;
		int columnCount = imageCount;
		int rowCount = 1;
		if (layout != null && !layout.isEmpty()) {
			if (layout.startsWith("[")) layout = layout.substring(1);
			if (layout.endsWith("]")) layout = layout.substring(0, layout.length()-1);
			String[] rows = layout.split(";");
			rowCount = rows.length;
			if (rows.length == 0) throw new IOException("Cannot montage, invalid layout: " + layout);
			columnCount = rows[0].trim().split(",").length;
		} else {
			StringBuilder sb = new StringBuilder();
			for (int i=1; i<=inputFiles.length; i++) {
				sb.append(i + ",");
			}
			sb.deleteCharAt(sb.length()-1);
			layout = sb.toString();
		}
		
		// Create an array of image names ordered by layout.
		String[] orderedInputFiles = new String[rowCount*columnCount];
		int index = 0;
		String[] rows = layout.split(";");
		for (int r=0; r<rowCount; r++) {
			String[] columns = rows[r].trim().split(",");
			for (int c=0; c<columnCount; c++) {
				String inputPath = null;
				int fieldIndex = Integer.parseInt(columns[c]);
				if (fieldIndex >= 0 && fieldIndex < inputFiles.length) inputPath = inputFiles[fieldIndex];
				orderedInputFiles[index++] = inputPath;
			}
		}
		
		montageInternal(orderedInputFiles, rowCount, columnCount, padding, outputFile);
	}
	
	public static String appendFrameNr(String fileName, int frameNr) {
		return fileName + "[frame=" + frameNr + "]";
	}
	
	private static int getFrameNr(String fileName) {
		Pattern pattern = Pattern.compile(".*\\[frame=(\\d+)\\]");
		Matcher matcher = pattern.matcher(fileName);
		if (matcher.matches()) return Integer.parseInt(matcher.group(1));
		return -1;
	}
	
	private static String removeFrameNr(String fileName) {
		if (getFrameNr(fileName) == -1) return fileName;
		int frameIndex = fileName.lastIndexOf("[frame=");
		return fileName.substring(0, frameIndex);
	}
	
	private static void montageInternal(String[] orderedInputFiles, int rows, int columns, int padding, String outputFile) throws IOException {
		//TODO Support for the padding argument
		
		int[] imageSize = null;
		for (String inputFile: orderedInputFiles) {
			if (inputFile != null) {
				imageSize = ImageIdentifier.identify(removeFrameNr(inputFile));
				break;
			}
		}
		if (imageSize == null) throw new IOException("No valid input files found");
		
		int montageImageWidth = imageSize[0]*columns;
		int montageImageHeight = imageSize[1]*rows;
		int imageDepth = imageSize[3];
		
		PaletteData palette = new PaletteData(0xFF, 0xFF, 0xFF);
		ImageData montageImage = new ImageData(montageImageWidth, montageImageHeight, imageDepth, palette);
		
		int index = 0;
		for (int r=0; r<rows; r++) {
			for (int c=0; c<columns; c++) {
				String fileName = orderedInputFiles[index++];
				if (fileName == null) continue;
				
				int frameNr = getFrameNr(fileName);
				if (frameNr == -1) frameNr = 0;
				fileName = removeFrameNr(fileName);
				
				ImageData[] image = TIFFCodec.read(fileName);
				if (image.length <= frameNr) continue;
				ImageData imageToAdd = image[frameNr];
				
				for (int line=0; line<imageToAdd.height; line++) {
					int[] pixels = new int[imageToAdd.width];
					imageToAdd.getPixels(0, line, pixels.length, pixels, 0);
					
					int offsetX = c*imageSize[0];
					int offsetY = r*imageSize[1]+line;
					montageImage.setPixels(offsetX, offsetY, pixels.length, pixels, 0);
				}
			}
		}
		
		TIFFCodec.write(montageImage, outputFile);
	}
}
