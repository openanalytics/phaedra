package eu.openanalytics.phaedra.base.imaging.overlay.freeform.event;

import org.eclipse.core.commands.common.EventManager;
import org.eclipse.swt.graphics.PathData;

public class FreeFormEventManager extends EventManager {
	
	public void addListener(FreeFormListener listener) {
		addListenerObject(listener);
	}
	
	public void removeListener(FreeFormListener listener) {
		removeListenerObject(listener);
	}
	
	public void shapeStarted(int x, int y) {
		for (Object listener: getListeners()) {
			((FreeFormListener)listener).shapeStarted(x, y);
		}
	}
	
	public void shapeResumed(int x, int y) {
		for (Object listener: getListeners()) {
			((FreeFormListener)listener).shapeResumed(x, y);
		}
	}
	
	public void shapeFinished(PathData pathData) {
		for (Object listener: getListeners()) {
			((FreeFormListener)listener).shapeFinished(pathData);
		}
	}
}