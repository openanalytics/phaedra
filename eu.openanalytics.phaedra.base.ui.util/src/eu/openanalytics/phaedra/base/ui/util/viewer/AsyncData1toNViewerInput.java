package eu.openanalytics.phaedra.base.ui.util.viewer;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.StructuredViewer;

import eu.openanalytics.phaedra.base.ui.util.misc.AsyncDataLoader;


/**
 * {@link AsyncDataViewerInput} with mapping TEntity:TViewerElement = 1:N.
 * 
 * @param <TEntity> the type of elements in the input
 * @param <TViewerElement> the type of elements in the viewer
 */
public abstract class AsyncData1toNViewerInput<TEntity, TViewerElement> extends AsyncDataViewerInput<TEntity, TViewerElement> {
	
	
	private final Class<TViewerElement> viewerElementType;
	
	private volatile List<TViewerElement> viewerElements;
	
	
	public AsyncData1toNViewerInput(final Class<TEntity> baseElementType, final Class<TViewerElement> viewerElementType,
			final AsyncDataLoader<TEntity> dataLoader) {
		super(baseElementType, dataLoader, false);
		this.viewerElementType = viewerElementType;
		init();
	}
	
	
	@Override
	public Class<TViewerElement> getViewerElementType() {
		return this.viewerElementType;
	}
	
	@Override
	public List<TViewerElement> getViewerElements() {
		return this.viewerElements;
	}
	
	@Override
	public abstract TEntity getBaseElement(final TViewerElement element);
	
	public abstract List<TViewerElement> getViewerElements(final TEntity baseElement);
	
	public int getViewerElementsSize(final TEntity baseElement) {
		return getViewerElements(baseElement).size();
	}
	
	public int getViewerElementIndexOf(final TEntity baseElement, final TViewerElement element) {
		return getViewerElements(baseElement).indexOf(element);
	}
	
	
	@Override
	protected void setElements(final List<TEntity> elements) {
		final ArrayList<TViewerElement> viewerElements = new ArrayList<TViewerElement>();
		if (!elements.isEmpty()) {
			for (final TEntity element : elements) {
				viewerElements.addAll(getViewerElements(element));
			}
		}
		
		super.setElements(elements);
		this.viewerElements = viewerElements;
	}
	
	@Override
	public void refreshViewer(final List<? extends TEntity> elements) {
		final StructuredViewer viewer = getViewer();
		if (viewer == null || viewer.getControl().isDisposed()) {
			return;
		}
		viewer.getControl().setRedraw(false);
		try {
			for (final TEntity element : elements) {
				for (final TViewerElement viewerElement : getViewerElements(element)) {
					viewer.refresh(viewerElement);
				}
			}
		} finally {
			viewer.getControl().setRedraw(true);
		}
	}
	
}
