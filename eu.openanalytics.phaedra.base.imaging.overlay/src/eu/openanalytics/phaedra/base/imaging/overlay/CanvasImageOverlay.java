package eu.openanalytics.phaedra.base.imaging.overlay;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.ISelectionListener;

public class CanvasImageOverlay implements IImageOverlay {

	private Canvas canvas;
	
	public void setCanvas(Canvas canvas) {
		this.canvas = canvas;
	}
	
	protected Canvas getCanvas() {
		return canvas;
	}
	
	@Override
	public void render(GC gc) {
		// Default: render nothing.
	}
	
	@Override
	public MouseListener getMouseListener() {
		// Default: no listener.
		return null;
	}
	
	@Override
	public MouseMoveListener getMouseMoveListener() {
		// Default: no listener.
		return null;
	}
	
	@Override
	public KeyListener getKeyListener() {
		// Default: no listener.
		return null;
	}
	
	@Override
	public boolean overridesMouseEvents(int x, int y) {
		// Default: override nothing.
		return false;
	}
	
	@Override
	public ISelectionListener[] getSelectionListeners() {
		// Default: no listener.
		return null;
	}
	
	@Override
	public ISelectionProvider getSelectionProvider() {
		// Default: no provider.
		return null;
	}
	
	@Override
	public void createButtons(ToolBar parent) {
		// Default: do nothing.
	}
	
	@Override
	public void createContextMenu(IMenuManager manager) {
		// Default: do nothing.
	}
	
	@Override
	public void setCurrentMouseMode(int currentMouseMode) {
		// Default: do nothing.
	}
	
	@Override
	public void dispose() {
		// Default: nothing to dispose.
	}
}
