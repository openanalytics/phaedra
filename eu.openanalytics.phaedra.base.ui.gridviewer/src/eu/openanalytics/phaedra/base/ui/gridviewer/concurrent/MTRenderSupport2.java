package eu.openanalytics.phaedra.base.ui.gridviewer.concurrent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

import eu.openanalytics.phaedra.base.cache.CacheKey;
import eu.openanalytics.phaedra.base.cache.CacheService;
import eu.openanalytics.phaedra.base.cache.ICache;
import eu.openanalytics.phaedra.base.environment.prefs.PrefUtils;
import eu.openanalytics.phaedra.base.ui.gridviewer.Activator;
import eu.openanalytics.phaedra.base.ui.gridviewer.widget.Grid;
import eu.openanalytics.phaedra.base.ui.gridviewer.widget.GridCell;
import eu.openanalytics.phaedra.base.util.threading.ConcurrentTask;
import eu.openanalytics.phaedra.base.util.threading.ThreadPool;

/**
 * Multithreading helper for {@link BaseConcurrentGridCellRenderer}.
 * 
 * Note that all methods are 'package private' to indicate that this class
 * should <b>not</b> be used outside of the cell renderer.
 */
public class MTRenderSupport2 {

	private Grid grid;

	private int currentWidth;
	private int currentHeight;
	
	private ThreadPool pool;
	private List<Future<?>> runningTasks;

	public MTRenderSupport2(Grid grid) {
		this.grid = grid;
		pool = new ThreadPool(PrefUtils.getNumberOfThreads());
		runningTasks = new ArrayList<>();
	}

	/* package */ public boolean isRendering() {
		return (pool.getCompletedCount() == 0 || pool.getWorkload() > 0);
	}

	/* package */ public void resetRendering() {
		cancelCurrentTasks();
		clearCache();
	}

	/* package */ void offloadTask(ConcurrentTask task) {
		Runnable runnable = () -> {
			try {
				task.run();
			} catch (Throwable t) {
				Activator.getDefault().getLog().log(new Status(
						IStatus.ERROR, Activator.PLUGIN_ID, "Failed to run layer task", t));
			}
			handleResult(task);
		};
		runningTasks.add(pool.schedule(runnable));
	}

	/* package */ ImageData getImageData(GridCell cell) {
		return (ImageData) getCache().get(getCacheKey(cell.getRow(), cell.getColumn()));
	}
	
	/* package */ boolean isValidCache(int width, int height) {
		return width == currentWidth && height == currentHeight;
	}
	
	/* package */ public synchronized void cancelCurrentTasks() {
		for (Future<?> task : runningTasks) task.cancel(false);
		runningTasks.clear();
	}
	
	/* package */ void reset(int w, int h) {
		currentWidth = w;
		currentHeight = h;
		resetRendering();
	}
	
	/* package */ void dispose() {
 		resetRendering();
 		pool.stop(false);
	}
	
	/*
	 * Non-public
	 * **********
	 */
	
	private void handleResult(ConcurrentTask task) {
		Object result = task.getResult();
		if (result instanceof ConcurrentTaskResult) {
			ConcurrentTaskResult r = (ConcurrentTaskResult)task.getResult();
			putIfValid(r.getRow(), r.getColumn(), r.getImageData());
		} else {
			// Ignore incompatible result types.
		}
	}
	
	private void putIfValid(final int row, final int col, ImageData data) {
		if (isValidCache(data.width, data.height)) {
			CacheKey cacheKey = getCacheKey(row, col);
			if (!getCache().contains(cacheKey)) {
				getCache().put(cacheKey, data);
				Display.getDefault().asyncExec(() -> redraw(row, col));
			}
		}
	}

	private void redraw(int row, int col) {
		if (grid.isDisposed()) return;

		if (row >= grid.getRows() || col >= grid.getColumns()) return;
		GridCell bodyCell = grid.getCell(row, col);

		Rectangle bounds = grid.calculateBounds(bodyCell);
		GC gc = null;
		try {
			gc = new GC(grid);
			bodyCell.paint(gc, bounds.x,bounds.y,bounds.width,bounds.height);
		} finally {
			gc.dispose();
		}
	}
	
	private void clearCache() {
		for (int i = 0; i < grid.getRows(); i++) {
			for (int j = 0; j < grid.getColumns(); j++) {
				getCache().remove(getCacheKey(i, j));
			}
		}
	}

	private ICache getCache() {
		return CacheService.getInstance().getDefaultCache();
	}

	private CacheKey getCacheKey(int row, int col) {
		return CacheKey.create(hashCode(), grid.getCell(row, col).hashCode() , row, col);
	}
}
