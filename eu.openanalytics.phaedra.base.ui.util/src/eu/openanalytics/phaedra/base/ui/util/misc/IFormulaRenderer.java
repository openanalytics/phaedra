package eu.openanalytics.phaedra.base.ui.util.misc;

import java.io.ByteArrayInputStream;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.Point;

import eu.openanalytics.phaedra.base.util.misc.FormulaDescriptor;
import eu.openanalytics.phaedra.base.util.misc.ImageUtils;

//FIXME disabled SVG rendering while the Batik classloading issue is unresolved (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=549920)
public interface IFormulaRenderer {

	public default boolean canRenderFormula(FormulaDescriptor descriptor) {
//		return descriptor != null && (descriptor.getSvg() != null || descriptor.getPng() != null);
		return descriptor != null && descriptor.getPng() != null;
	}
	
	public default Image renderFormula(FormulaDescriptor descriptor, Point size, Color backgroundColor) {
		if (!canRenderFormula(descriptor)) return null;
		Image image = null;
		
//		if (descriptor.getSvg() != null) {
//			image = ImageUtils.getSVGAsImageMaxSize(descriptor.getSvg(), size.x, size.y, backgroundColor);
//		} else 
		if (descriptor.getPng() != null) {
			ImageData[] imageData = new ImageLoader().load(new ByteArrayInputStream(descriptor.getPng()));
			if (imageData != null && imageData.length > 0) image = new Image(null, imageData[0]);
			image = ImageUtils.scaleByAspectRatio(image, size.x, size.y, true);
		}
		return image;
	}
}
