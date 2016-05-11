package eu.openanalytics.phaedra.base.imaging.overlay.freeform;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.graphics.Path;
import org.eclipse.swt.graphics.PathData;

import eu.openanalytics.phaedra.base.imaging.overlay.freeform.event.FreeFormEventManager;

public abstract class BaseFormProvider implements IFreeFormProvider {

	private FreeFormEventManager eventManager;
	private IPointTranslator pointTranslator;
	
	private List<PathData> finishedPaths;
	private Path currentPath;
	
	public BaseFormProvider() {
		finishedPaths = new ArrayList<>();
	}
	
	@Override
	public void onMouseDown(int x, int y) {
		// Default: do nothing.
	}

	@Override
	public void onMouseUp(int x, int y) {
		// Default: do nothing.
	}

	@Override
	public void onMouseMove(int x, int y) {
		// Default: do nothing.
	}
	
	@Override
	public void onKeyPress(int keyCode) {
		// Default: do nothing.
	}
	
	@Override
	public void onKeyRelease(int keyCode) {
		// Default: do nothing.
	}
	
	@Override
	public void undo() {
		List<PathData> paths = getPathData();
		if (paths.isEmpty()) return;
		paths.remove(paths.size()-1);
	}
	
	@Override
	public void reset() {
		getPathData().clear();
	}
	
	public void setEventManager(FreeFormEventManager eventManager) {
		this.eventManager = eventManager;
	}
	
	public FreeFormEventManager getEventManager() {
		return eventManager;
	}

	public IPointTranslator getPointTranslator() {
		return pointTranslator;
	}
	
	public void setPointTranslator(IPointTranslator pointTranslator) {
		this.pointTranslator = pointTranslator;
	}
	
	/*
	 * Non-public
	 * **********
	 */
	
	protected List<PathData> getPathData() {
		return finishedPaths;
	}
	
	protected Path getCurrentPath() {
		return currentPath;
	}
	
	protected void setCurrentPath(Path currentPath) {
		this.currentPath = currentPath;
	}
}
