package eu.openanalytics.phaedra.base.imaging.overlay.freeform;

import org.eclipse.swt.graphics.Point;

public interface IPointTranslator {

	public Point screenToImage(int x, int y);
	
	public Point imageToScreen(int x, int y);
}
