package eu.openanalytics.phaedra.base.ui.util.viewer;

import java.util.Comparator;

import eu.openanalytics.phaedra.base.ui.util.misc.AsyncDataLoader;
import eu.openanalytics.phaedra.base.ui.util.misc.DataLoadStatus;


/**
 * Comparator for async loaded data.
 * 
 * @param <TEntity> the type of elements in the viewer input and data loader
 * @param <TViewerElement> the type of elements in the viewer
 * @param <TData> the type of the loaded data
 * 
 * @see AsyncDataDirectComparator
 */
public abstract class AsyncDataComparator<TEntity, TViewerElement, TData> implements Comparator<TViewerElement> {
	
	
	protected final AsyncDataLoader<TEntity>.DataAccessor<TData> dataAccessor;
	
	
	public AsyncDataComparator(final AsyncDataLoader<TEntity>.DataAccessor<TData> dataAccessor) {
		this.dataAccessor = dataAccessor;
	}
	
	
	protected abstract Object getData(final TViewerElement element);
	
	@Override
	public int compare(final TViewerElement element1, final TViewerElement element2) {
		final Object r1 = getData(element1);
		final Object r2 = getData(element2);
		if (r1 == r2) {
			return 0;
		}
		if (r1 instanceof DataLoadStatus) {
			if (r2 instanceof DataLoadStatus) {
				return 0;
			} else {
				return 1;
			}
		} else {
			if (r2 instanceof DataLoadStatus) {
				return -1;
			} else {
				return compareData(element1, (TData)r1, element2, (TData)r2);
			}
		}
	}
	
	protected abstract int compareData(final TViewerElement element1, final TData data1,
			final TViewerElement element2, final TData data2);
	
}
