package eu.openanalytics.phaedra.base.ui.gridviewer.concurrent;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

import eu.openanalytics.phaedra.base.cache.CacheKey;
import eu.openanalytics.phaedra.base.cache.CacheService;
import eu.openanalytics.phaedra.base.cache.ICache;
import eu.openanalytics.phaedra.base.ui.gridviewer.widget.Grid;
import eu.openanalytics.phaedra.base.ui.gridviewer.widget.GridCell;
import eu.openanalytics.phaedra.base.util.threading.ThreadPool;

public class MTRenderSupport {

	private ThreadPool threadPool;
	private Grid grid;

	private Set<GridCell> rendersInProgress;

	private ImageCreator imageCreator;

	private int currentWidth;
	private int currentHeight;

	/**
	 * @deprecated Use {@link BaseConcurrentGridCellRenderer} instead.
	 */
	public MTRenderSupport(int threads, Grid grid) {
		this.threadPool = new ThreadPool(threads);
		this.grid = grid;
		this.rendersInProgress = new HashSet<GridCell>();
	}

	public void setImageCreator(ImageCreator imageCreator) {
		this.imageCreator = imageCreator;
	}

	public ImageData getImageData(GridCell cell, int w, int h) {

		if (w != currentWidth || h != currentHeight) {
			abortAll();
			currentWidth = w;
			currentHeight = h;
		}

		CacheKey key = getCacheKey(cell.getRow(), cell.getColumn());
		if (getCache().contains(key)) return (ImageData) getCache().get(key);
		
		if (!rendersInProgress.contains(cell)) {
			rendersInProgress.add(cell);
			threadPool.schedule(new ImageBuilder(cell, w, h));
		}
		return null;
	}

	public void dispose() {
		threadPool.stop(false);
		clearCache();
	}

	public void abortAll() {
		// 1. Clear the queue
		threadPool.clearQueue();
		// 2. Ignore currently running tasks
		rendersInProgress.clear();
		// 3. Clear cache
		clearCache();
	}

	public void cancelCurrentTasks() {
		threadPool.clearQueue();
		rendersInProgress.clear();
	}

	public boolean isRendering() {
		return rendersInProgress.toArray().length > 0;
	}

	private void clearCache() {
		for (int i = 0; i < grid.getRows(); i++) {
			for (int j = 0; j < grid.getColumns(); j++) {
				getCache().remove(getCacheKey(i, j));
			}
		}
	}

	private void imageReady(final GridCell cell, ImageData data) {
		if (rendersInProgress.contains(cell)) {
			getCache().put(getCacheKey(cell.getRow(), cell.getColumn()), data);
			rendersInProgress.remove(cell);
		}
		Display.getDefault().asyncExec(() -> redraw(cell));
	}

	private void redraw(GridCell cell) {
		Grid grid = cell.getGrid();
		if (grid.isDisposed()) return;

		Rectangle bounds = grid.calculateBounds(cell);
		GC gc = null;
		try {
			gc = new GC(grid);
			cell.paint(gc, bounds.x, bounds.y, bounds.width, bounds.height);
		} finally {
			gc.dispose();
		}
	}

	private ICache getCache() {
		return CacheService.getInstance().getDefaultCache();
	}
	
	private CacheKey getCacheKey(int row, int col) {
		return CacheKey.create(hashCode(), grid.getCell(row, col).hashCode() , row, col);
	}

	private class ImageBuilder implements Callable<ImageData> {

		private GridCell cell;
		private int w, h;

		public ImageBuilder(GridCell cell, int w, int h) {
			this.cell = cell;
			this.w = w;
			this.h = h;
		}

		@Override
		public ImageData call() throws Exception {
			ImageData data = imageCreator.createImageData(cell, w, h);
			imageReady(cell, data);
			return data;
		}
	}

	public static interface ImageCreator {
		public ImageData createImageData(GridCell cell, int w, int h);
	}
}
