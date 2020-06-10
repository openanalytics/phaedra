package eu.openanalytics.phaedra.base.ui.util.viewer;

import java.util.Objects;
import java.util.function.Function;

import org.eclipse.swt.graphics.Image;

import eu.openanalytics.phaedra.base.ui.util.misc.AsyncDataLoader;


/**
 * {@link AsyncDataLabelProvider} with TEntity == TViewerElement.
 * 
 * @param <TEntity> the type of elements in the base input, the viewer and the data loader
 * @param <TData> the type of the loaded data
 * 
 * @see AsyncDataLabelProvider
 */
public class AsyncDataDirectLabelProvider<TEntity, TData> extends AsyncDataLabelProvider<TEntity, TEntity, TData> {
	
	
	public AsyncDataDirectLabelProvider(final AsyncDataLoader<TEntity>.DataAccessor<TData> dataAccessor) {
		super(dataAccessor);
	}
	
	
	@Override
	protected Object getData(final TEntity element) {
		return this.dataAccessor.getData(element);
	}
	
	
	public static <E, D> AsyncDataDirectLabelProvider<E, D> providingText(final AsyncDataLoader<E>.DataAccessor<D> dataAccessor,
			final Function<Object, String> textFunction) {
		Objects.requireNonNull(textFunction);
		return new AsyncDataDirectLabelProvider<E, D>(dataAccessor) {
			@Override
			protected String getText(final E element, final D data) {
				final String text = textFunction.apply(data);
				return (text != null) ? text : "";
			}
		};
	}
	
	public static <E, D> AsyncDataDirectLabelProvider<E, D> providingTextImage(final AsyncDataLoader<E>.DataAccessor<D> dataAccessor,
			final Function<Object, String> textFunction, final Function<Object, Image> imageFunction) {
		Objects.requireNonNull(textFunction);
		return new AsyncDataDirectLabelProvider<E, D>(dataAccessor) {
			@Override
			protected String getText(final E element, final D data) {
				final String text = textFunction.apply(data);
				return (text != null) ? text : "";
			}
			@Override
			protected Image getImage(final E element, final D data) {
				return imageFunction.apply(data);
			}
		};
	}
	
}
