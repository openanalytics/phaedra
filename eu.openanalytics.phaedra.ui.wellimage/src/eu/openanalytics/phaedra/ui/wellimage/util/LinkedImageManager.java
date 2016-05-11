package eu.openanalytics.phaedra.ui.wellimage.util;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;

import eu.openanalytics.phaedra.ui.wellimage.util.JP2KImageCanvas.JP2KImageCanvasListener;


/**
 * Links a number of JP2KImageCanvas objects together, allowing the user
 * to pan and zoom the canvases simultaneously.
 */
public class LinkedImageManager {

	private static LinkedImageManager instance;
	
	private List<JP2KImageCanvas> linkedCanvases;
	
	private LinkedImageManager() {
		// Hidden constructor.
		linkedCanvases = new ArrayList<JP2KImageCanvas>();
	}
	
	public static LinkedImageManager getInstance() {
		if (instance == null) instance = new LinkedImageManager();
		return instance;
	}
	
	public void addCanvas(JP2KImageCanvas canvas) {
		if (linkedCanvases.contains(canvas)) return;
		syncState(canvas);
		enableLink(canvas);
		linkedCanvases.add(canvas);
	}
	
	public void removeCanvas(JP2KImageCanvas canvas) {
		if (!linkedCanvases.contains(canvas)) return;
		disableLink(canvas);
		linkedCanvases.remove(canvas);
	}
	
	public boolean isLinked(JP2KImageCanvas canvas) {
		// Return true if the canvas is linked to at least one other canvas.
		return (linkedCanvases.contains(canvas) && linkedCanvases.size() > 1);
	}
	
	/*
	 * Non-public
	 * **********
	 */
	
	private void syncState(JP2KImageCanvas newCanvas) {
		// Make sure the new canvas has the same scale as the other canvases.
		if (!linkedCanvases.isEmpty()) {
			float newScale = linkedCanvases.get(0).getCurrentScale();
			newCanvas.changeScale(newScale);
		}
	}
	
	private void enableLink(final JP2KImageCanvas canvas) {
		CanvasListener listener = new CanvasListener(canvas);
		canvas.setData("linkedListener", listener);
		canvas.addListener(listener);
		canvas.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				removeCanvas(canvas);
			}
		});
	}
	
	private void disableLink(JP2KImageCanvas canvas) {
		CanvasListener listener = (CanvasListener)canvas.getData("linkedListener");
		if (listener != null) canvas.removeListener(listener);
	}

	private class CanvasListener implements JP2KImageCanvasListener {
		
		private JP2KImageCanvas host;
		
		public CanvasListener(JP2KImageCanvas host) {
			this.host = host;
		}
		
		@Override
		public void onFileChange() {
			// Do nothing.
		}

		@Override
		public void onScaleChange(float newScale) {
			for (JP2KImageCanvas canvas: linkedCanvases) {
				if (host == canvas) continue;
				canvas.changeScale(newScale);
			}
		}

		@Override
		public void onOffsetChange(int x, int y) {
			for (JP2KImageCanvas canvas: linkedCanvases) {
				if (host == canvas) continue;
				canvas.changeImageOffset(x, y);
			}
		}
		
	}
}
