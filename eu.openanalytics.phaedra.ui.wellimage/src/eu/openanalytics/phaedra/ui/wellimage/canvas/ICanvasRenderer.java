package eu.openanalytics.phaedra.ui.wellimage.canvas;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.ui.services.IDisposable;

public interface ICanvasRenderer extends IDisposable {

	public default void initialize(ICanvasRenderCallback callback) {}
	
	public void drawImage(GC gc, Rectangle clientArea, CanvasState canvasState);
	
}
