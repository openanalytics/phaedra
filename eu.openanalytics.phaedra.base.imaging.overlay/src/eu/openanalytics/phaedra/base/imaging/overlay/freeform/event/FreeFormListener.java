package eu.openanalytics.phaedra.base.imaging.overlay.freeform.event;

import org.eclipse.swt.graphics.PathData;

public class FreeFormListener {
	
	public void shapeStarted(int x, int y) {
		// Default behaviour: do nothing.
	}
	
	public void shapeResumed(int x, int y) {
		// Default behaviour: do nothing.
	}
	
	public void shapeFinished(PathData pathData) {
		// Default behaviour: do nothing.
	}
}
