package eu.openanalytics.phaedra.ui.wellimage.util;

import java.io.IOException;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.Path;
import org.eclipse.swt.graphics.PathData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Transform;

import eu.openanalytics.phaedra.base.imaging.jp2k.CodecFactory;
import eu.openanalytics.phaedra.base.imaging.jp2k.IDecodeAPI;
import eu.openanalytics.phaedra.base.util.misc.ColorStore;
import eu.openanalytics.phaedra.calculation.ClassificationService;
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.vo.FeatureClass;

public class LabelImageFactory {

	public static ImageData createLabelImage(List<PathData> regions, List<FeatureClass> featureClasses,
			Well well, int channelNr, IProgressMonitor monitor) throws IOException {
		
		monitor.beginTask("Creating label image", 10);
		
		// Open the existing image to find the image size.
		int imageNr = PlateUtils.getWellNr(well) - 1;
		int channelCount = PlateUtils.getProtocolClass(well).getImageSettings().getImageChannels().size();
		IDecodeAPI currentImage = CodecFactory.getDecoder(PlateService.getInstance().getImagePath(well.getPlate()), PlateUtils.getWellCount(well.getPlate()), channelCount);
		currentImage.open();
		Point size = currentImage.getSize(imageNr);
		int depth = currentImage.getBitDepth(imageNr, channelNr);

		monitor.subTask("Drawing labels");

		// Use a smaller drawing size (SWT cannot instantiate Images > ~500 megapixel)
		int maxSize = 10000;
		float scaleFactor = 1.0f;
		while ((size.x*scaleFactor) > maxSize || (size.y*scaleFactor) > maxSize) {
			scaleFactor /= 2;
		}
		Point drawSize = new Point((int)(size.x*scaleFactor), (int)(size.y*scaleFactor));
		
		// Draw the new regions on top of the current label image.
		ColorStore colors = new ColorStore();
		Image image = new Image(null, drawSize.x, drawSize.y);
		GC gc = new GC(image);
		
		// Fill the image with black pixels.
		gc.setForeground(colors.get(new RGB(0,0,0)));
		gc.setBackground(colors.get(new RGB(0,0,0)));
		gc.fillRectangle(0, 0, drawSize.x, drawSize.y);
		gc.drawRectangle(0, 0, drawSize.x, drawSize.y);
		
		// Apply a transform so the regions are also drawn on a smaller scale.
		Transform t = new Transform(gc.getDevice());
		t.scale(scaleFactor, scaleFactor);
		gc.setTransform(t);
		
		// Draw the regions as filled polygons.
		for (int i=0; i<regions.size(); i++) {
			PathData region = regions.get(i);
			FeatureClass featureClass = featureClasses.get(i);
			
			// +1 because values 0 and 1 are reserved for background.
			int label = ClassificationService.getInstance().getNumericRepresentation(featureClass) + 1;
			
			Color c = colors.get(new RGB(label,label,label));
			gc.setForeground(c);
			gc.setBackground(c);
			
			Path p = new Path(gc.getDevice(), region);
			gc.fillPath(p);
			p.dispose();
		}
		colors.dispose();
		gc.dispose();
		t.dispose();
		
		ImageData rgbData = image.getImageData();
		image.dispose();
		
		// Convert into a lower-depth imagedata to conserve memory.
		ImageData newLabelImage = new ImageData(drawSize.x, drawSize.y, depth, new PaletteData(0xFF, 0xFF, 0xFF));
		for (int y=0; y<drawSize.y; y++) {
			int[] scanLine = new int[drawSize.x];
			rgbData.getPixels(0, y, drawSize.x, scanLine, 0);
			for (int x=0; x<drawSize.x; x++) {
				// Grab the green value from the RGB image.
				scanLine[x] = (scanLine[x] >> 8) & 0xFF;
			}
			newLabelImage.setPixels(0, y, drawSize.x, scanLine, 0);
		}
		rgbData = null;
		
		// Scale back to original size.
		newLabelImage = newLabelImage.scaledTo(size.x, size.y);
		
		monitor.worked(3);
		
		// Obtain the current label image, which will be merged with the new one.
		monitor.subTask("Merging label image with existing image");
		ImageData currentLabelImage = currentImage.renderImage(drawSize.x, drawSize.y, imageNr, channelNr);
		currentLabelImage = currentLabelImage.scaledTo(size.x, size.y);
		currentImage.close();
		
		monitor.worked(2);
		
		// Merge the images into one result image.
		//TODO Support 16bit overlays.
		PaletteData palette = new PaletteData(0xFF, 0xFF, 0xFF);
		ImageData result = new ImageData(size.x, size.y, depth, palette);
		
		int[] currentPixels = new int[result.width];
		int[] newPixels = new int[result.width];
		int[] resultPixels = new int[result.width];
		
		for (int y=0; y<result.height; y++) {
			currentLabelImage.getPixels(0, y, result.width, currentPixels, 0);
			newLabelImage.getPixels(0, y, result.width, newPixels, 0);
			
			for (int x=0; x<result.width; x++) {
				int oldPixel = currentPixels[x] & 0xFF;
				int newPixel = newPixels[x] & 0xFF;
				resultPixels[x] = (newPixel > 0) ? newPixel : oldPixel;
			}
			result.setPixels(0, y, result.width, resultPixels, 0);
		}
		
		monitor.worked(5);
		monitor.done();
		
		return result;
	}
}
