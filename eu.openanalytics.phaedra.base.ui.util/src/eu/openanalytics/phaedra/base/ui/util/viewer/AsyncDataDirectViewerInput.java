package eu.openanalytics.phaedra.base.ui.util.viewer;

import java.util.List;

import eu.openanalytics.phaedra.base.ui.util.misc.AsyncDataLoader;


/**
 * {@link AsyncDataViewerInput} with TEntity == TViewerElement.
 * 
 * @param <TEntity> the type of elements in the input and the viewer
 */
public abstract class AsyncDataDirectViewerInput<TEntity> extends AsyncDataViewerInput<TEntity, TEntity> {
	
	
	public AsyncDataDirectViewerInput(final Class<TEntity> type,
			final AsyncDataLoader<TEntity> dataLoader) {
		super(type, dataLoader);
	}
	
	public AsyncDataDirectViewerInput(final Class<TEntity> type) {
		super(type);
	}
	
	
	@Override
	public Class<TEntity> getViewerElementType() {
		return getBaseElementType();
	}
	
	@Override
	public List<TEntity> getViewerElements() {
		return getBaseElements();
	}
	
	@Override
	public final TEntity getBaseElement(final TEntity element) {
		return element;
	}
	
}
