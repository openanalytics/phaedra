package eu.openanalytics.phaedra.base.ui.gridviewer.concurrent;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Rectangle;

import eu.openanalytics.phaedra.base.ui.gridviewer.widget.Grid;
import eu.openanalytics.phaedra.base.ui.gridviewer.widget.GridCell;
import eu.openanalytics.phaedra.base.ui.gridviewer.widget.render.BaseGridCellRenderer;
import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.util.threading.ConcurrentTask;

/**
 * A cell renderer that performs render tasks in a multithreaded fashion.
 * This renderer is designed for image-type renderings, where the images are generated
 * by mulitple threads and handed back to the UI thread for painting onto the grid cells.
 */
public abstract class BaseConcurrentGridCellRenderer extends BaseGridCellRenderer {

	private MTRenderSupport2 renderingSupport;

	public BaseConcurrentGridCellRenderer(Grid grid) {
		this.renderingSupport = new MTRenderSupport2(grid);
	}

	@Override
	public void prerender(Grid grid) {
		Rectangle bounds = grid.calculateBounds(grid.getCell(0, 0));

		renderingSupport.cancelCurrentTasks();
		if (!renderingSupport.isValidCache(bounds.width, bounds.height)) {
			renderingSupport.reset(bounds.width, bounds.height);
		}
	}

	@Override
	public void render(GridCell cell, GC gc, int x, int y, int w, int h) {
		// If the requested dimensions do not match the current renderingSupport dimensions, abort.
		if (!renderingSupport.isValidCache(w, h)) return;
		
		ImageData data = renderingSupport.getImageData(cell);
		if (data == null) {
			if (cell.getData() == null) return;
			// Submit a rendering job and render a temporary placeholder
			ConcurrentTask task = createRendertask(cell, w, h);
			if (task != null) renderingSupport.offloadTask(task);
			gc.drawImage(IconManager.getIconImage("loading.gif"), x+(w/2)-8, y+(h/2)-8);
		} else {
			// Render cached image
			Image image = null;
			try {
				image = new Image(null, data);
				gc.drawImage(image, x, y);
			} finally {
				if (image != null) image.dispose();
			}
		}
	}

	@Override
	public void dispose() {
		renderingSupport.dispose();
		super.dispose();
	}

	public void resetRendering() {
		if (renderingSupport != null) renderingSupport.resetRendering();
	}
	
	public boolean isRendering() {
		return renderingSupport != null && renderingSupport.isRendering();
	}
	
	/**
	 * This method is called when a request arrives to paint a grid cell but no image
	 * is available for that cell.
	 * The returned {@link ConcurrentTask} is immediately scheduled for execution.
	 * The task's result should be an object of type {@link ConcurrentTaskResult}.
	 */
	protected abstract ConcurrentTask createRendertask(GridCell cell, int w, int h);

}