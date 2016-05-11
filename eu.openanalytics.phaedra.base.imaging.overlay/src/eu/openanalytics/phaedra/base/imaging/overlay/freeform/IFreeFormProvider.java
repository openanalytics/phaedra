package eu.openanalytics.phaedra.base.imaging.overlay.freeform;

import org.eclipse.swt.graphics.GC;

public interface IFreeFormProvider {

	public String getShapeName();
	
	public void onMouseDown(int x, int y);
	public void onMouseUp(int x, int y);
	public void onMouseMove(int x, int y);
	
	public void onKeyPress(int keyCode);
	public void onKeyRelease(int keyCode);
	
	public void undo();
	public void reset();
	
	public void draw(GC gc, boolean labelImg, int startingLabel);
}
