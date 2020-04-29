package eu.openanalytics.phaedra.base.ui.util.viewer;

import java.util.Comparator;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

import eu.openanalytics.phaedra.base.ui.util.misc.AsyncDataLoader;


/**
 * {@link AsyncDataComparator} with TEntity == TViewerElement
 * 
 * @param <TEntity> the type of elements in the viewer, base input and data loader
 * @param <TData> the type of the loaded data
 * 
 * @see AsyncDataComparator
 */
public abstract class AsyncDataDirectComparator<TEntity, TData> extends AsyncDataComparator<TEntity, TEntity, TData> {
	
	
	public AsyncDataDirectComparator(final AsyncDataLoader<TEntity>.DataAccessor<TData> dataAccessor) {
		super(dataAccessor);
	}
	
	
	@Override
	protected Object getData(final TEntity element) {
		return this.dataAccessor.getData(element);
	}
	
	
	public static <E, D> AsyncDataDirectComparator<E, D> comparing(final AsyncDataLoader<E>.DataAccessor<D> dataAccessor, final Comparator<D> comparator) {
		return new AsyncDataDirectComparator<E, D>(dataAccessor) {
			@Override
			protected int compareData(final E element1, final D data1, final E element2, final D data2) {
				return comparator.compare(data1, data2);
			}
		};
	}
	
	public static <E, D> AsyncDataDirectComparator<E, D> comparingInt(final AsyncDataLoader<E>.DataAccessor<D> dataAccessor, final ToIntFunction<D> f) {
		return new AsyncDataDirectComparator<E, D>(dataAccessor) {
			@Override
			protected int compareData(final E element1, final D data1, final E element2, final D data2) {
				return Integer.compare(f.applyAsInt(data1), f.applyAsInt(data2));
			}
		};
	}
	
	public static <E, D> AsyncDataDirectComparator<E, D> comparingLong(final AsyncDataLoader<E>.DataAccessor<D> dataAccessor, final ToLongFunction<D> f) {
		return new AsyncDataDirectComparator<E, D>(dataAccessor) {
			@Override
			protected int compareData(final E element1, final D data1, final E element2, final D data2) {
				return Long.compare(f.applyAsLong(data1), f.applyAsLong(data2));
			}
		};
	}
	
	public static <E, D> AsyncDataDirectComparator<E, D> comparingDouble(final AsyncDataLoader<E>.DataAccessor<D> dataAccessor, final ToDoubleFunction<D> f) {
		return new AsyncDataDirectComparator<E, D>(dataAccessor) {
			@Override
			protected int compareData(final E element1, final D data1, final E element2, final D data2) {
				return Double.compare(f.applyAsDouble(data1), f.applyAsDouble(data2));
			}
		};
	}
	
}
