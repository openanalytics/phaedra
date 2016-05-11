package eu.openanalytics.phaedra.base.ui.nattable.misc;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.command.VisualRefreshCommand;
import org.eclipse.nebula.widgets.nattable.data.IColumnPropertyAccessor;

import eu.openanalytics.phaedra.base.cache.CacheKey;
import eu.openanalytics.phaedra.base.cache.CacheService;
import eu.openanalytics.phaedra.base.cache.ICache;
import eu.openanalytics.phaedra.base.db.IValueObject;
import eu.openanalytics.phaedra.base.environment.prefs.PrefUtils;
import eu.openanalytics.phaedra.base.ui.util.misc.RefreshManager;
import eu.openanalytics.phaedra.base.util.threading.ThreadUtils;

/**
 * <p>
 * An implementation of {@link IColumnPropertyAccessor} that loads data asynchronously.
 * This means that {@link #getDataValue(Object, int)} returns quickly, without blocking the UI thread.
 * But if the value is not available yet (i.e. is expensive to obtain or calculate), null is returned.
 * </p><p>
 * In the meantime, the expensive loading operations are performed in a background thread, which periodically
 * refreshes the table to display new content.
 * </p><p>
 * You can mix sync and async columns by handling the sync columns in <code>getDataValue</code>, and the
 * async columns in <code>loadDataValue</code>.
 * </p>
 * <p>
 * Notes:
 * <ul>
 * <li>If <code>setTable</code> is not called, the background job will not automatically refresh the table</li>
 * <li>Remember to <code>dispose</code> this class when it's no longer used</li>
 * </ul>
 * </p>
 */
public class AsyncColumnAccessor<T> implements IAsyncColumnAccessor<T> {

	private NatTable table;

	private ExecutorService executor;
	private RefreshManager refreshManager;

	private boolean isAsync;

	public AsyncColumnAccessor() {
		isAsync = true;
		executor = Executors.newFixedThreadPool(PrefUtils.getNumberOfThreads());
		refreshManager = new RefreshManager(1000, () -> {
			if (table != null && !table.isDisposed()) {
				table.doCommand(new VisualRefreshCommand());
			}
		});
		refreshManager.start();
	}

	public void setTable(NatTable table) {
		this.table = table;
	}

	@Override
	public int getColumnCount() {
		return 0;
	}

	@Override
	public Object getDataValue(T rowObject, int colIndex) {
		CacheKey key = getKey(rowObject, colIndex);
		if (getCache().contains(key)) return getCache().get(key);
		else {
			if (isAsync) {
				// Set a temporary value so doLoadValue is called only once.
				getCache().put(key, null);
				doLoadValue(rowObject, colIndex);
				return null;
			} else {
				Object value = loadDataValue(rowObject, colIndex);
				getCache().put(key, value);
				return value;
			}
		}
	}

	@Override
	public void setDataValue(T rowObject, int colIndex, Object newValue) {
		// Default: do nothing
	}

	@Override
	public int getColumnIndex(String colName) {
		return 0;
	}

	@Override
	public String getColumnProperty(int colIndex) {
		return null;
	}

	public void reset(int colIndex) {
		getCache().remove(getKey(null, colIndex), true);
	}

	public void reset() {
		getCache().remove(getKey(null, null), true);
	}

	public void dispose() {
		reset();
		executor.shutdownNow();
		refreshManager.stop();
	}

	@Override
	public void setAsync(boolean isAsync) {
		if (this.isAsync && !isAsync && executor instanceof ThreadPoolExecutor) {
			// When disabling async behaviour, running async tasks will cause incorrect sorting. Wait if necessary.
			ThreadUtils.waitUntilIdle((ThreadPoolExecutor)executor);
		}
		this.isAsync = isAsync;
	}

	/*
	 * Non-public
	 * **********
	 */

	protected Object loadDataValue(T rowObject, int colIndex) {
		return null;
	}

	private void doLoadValue(T rowObject, int colIndex) {
		executor.submit(() -> {
			Object value = loadDataValue(rowObject, colIndex);
			getCache().put(getKey(rowObject, colIndex), value);
			refreshManager.requestRefresh();
		});
	}

	private ICache getCache() {
		return CacheService.getInstance().getDefaultCache();
	}
	
	private CacheKey getKey(T row, Integer colIndex) {
		if (row instanceof IValueObject) {
			// Optimization for serialization: id instead of whole object
			return CacheKey.create(table.hashCode(), row.getClass(), ((IValueObject) row).getId(), colIndex);
		} else {
			return CacheKey.create(table.hashCode(), row == null ? null: row.getClass(), row, colIndex);
		}
	}
}
