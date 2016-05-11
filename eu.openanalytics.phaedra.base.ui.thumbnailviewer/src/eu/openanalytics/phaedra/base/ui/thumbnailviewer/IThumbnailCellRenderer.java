package eu.openanalytics.phaedra.base.ui.thumbnailviewer;

import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Rectangle;

public interface IThumbnailCellRenderer {

	public ImageData getImageData(Object o);

	public Rectangle getImageBounds(Object o);

	public boolean isImageReady(Object o);

}