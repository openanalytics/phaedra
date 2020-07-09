package eu.openanalytics.phaedra.base.ui.util.viewer;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.swt.widgets.Display;

import eu.openanalytics.phaedra.base.event.IModelEventListener;
import eu.openanalytics.phaedra.base.event.ModelEvent;
import eu.openanalytics.phaedra.base.event.ModelEventService;
import eu.openanalytics.phaedra.base.event.ModelEventType;
import eu.openanalytics.phaedra.base.ui.util.misc.AsyncDataLoader;


/**
 * Manages the model input for viewers.
 * It updates the input on change events of the {@link ModelEventService} and optionally schedules
 * the asynchronous loading of data for the input elements using {@link AsyncDataLoader}.
 * 
 * @param <TEntity> the type of elements in the input
 * @param <TViewerElement> the type of elements in the viewer
 */
public abstract class AsyncDataViewerInput<TEntity, TViewerElement> extends WritableValue<List<TEntity>>
		implements AsyncDataLoader.Listener<TEntity> {
	
	
	private final Class<TEntity> baseElementType;
	
	private volatile List<TEntity> baseElements;
	
	private final AsyncDataLoader<TEntity> dataLoader;
	
	private final IModelEventListener modelListener;
	
	private StructuredViewer viewer;
	
	
	protected AsyncDataViewerInput(final Class<TEntity> baseElementType,
			final AsyncDataLoader<TEntity> dataLoader, final boolean init) {
		this.baseElementType = baseElementType;
		this.dataLoader = dataLoader;
		if (dataLoader != null) {
			dataLoader.addListener(this);
		}
		
		this.modelListener = new ModelListener();
		ModelEventService.getInstance().addEventListener(this.modelListener);
		
		if (init) {
			init();
		}
	}
	
	public AsyncDataViewerInput(final Class<TEntity> baseElementType,
			final AsyncDataLoader<TEntity> dataLoader) {
		this(baseElementType, dataLoader, true);
	}
	
	public AsyncDataViewerInput(final Class<TEntity> baseElementType) {
		this(baseElementType, null, true);
	}
	
	protected void init() {
		setElements(loadElements());
		updateInput();
	}
	
	@Override
	public void dispose() {
		ModelEventService.getInstance().removeEventListener(this.modelListener);
		this.viewer = null;
		
		if (this.dataLoader != null) {
			this.dataLoader.removeListener(this);
		}
	}
	
	
	public StructuredViewer getViewer() {
		return this.viewer;
	}
	
	public AsyncDataLoader<TEntity> getDataLoader() {
		return this.dataLoader;
	}
	
	public void connect(final StructuredViewer viewer) {
		this.viewer = viewer;
		
		final AsyncDataLoader<TEntity> dataLoader = this.dataLoader;
		if (dataLoader != null) {
			dataLoader.asyncLoad(this.baseElements, null);
		}
		
		viewer.setUseHashlookup(true);
		viewer.setInput(getViewerElements());
	}
	
	public void reload(final Collection<String> properties) {
		final AsyncDataLoader<TEntity> dataLoader = this.dataLoader;
		if (dataLoader != null) {
			dataLoader.asyncLoad(this.baseElements, properties);
		}
		
		refreshViewer();
	}
	
	
	public final Class<TEntity> getBaseElementType() {
		return this.baseElementType;
	}
	
	public List<TEntity> getBaseElements() {
		return this.baseElements;
	}
	
	public abstract Class<TViewerElement> getViewerElementType();
	
	public abstract List<TViewerElement> getViewerElements();
	
	public abstract TEntity getBaseElement(final TViewerElement element);
	
	@Override
	public void doSetValue(final List<TEntity> value) {
		throw new UnsupportedOperationException();
	}
	
	
	protected abstract List<TEntity> loadElements();
	
	protected void setElements(final List<TEntity> elements) {
		this.baseElements = elements;
	}
	
	private class ModelListener implements IModelEventListener {
		@Override
		public void handleEvent(final ModelEvent event) {
			final boolean structChanged = event.type == ModelEventType.ObjectCreated
					|| event.type == ModelEventType.ObjectChanged
					|| event.type == ModelEventType.ObjectRemoved;
			if (!structChanged) {
				return;
			}
			
			final List<TEntity> currentElements = AsyncDataViewerInput.this.baseElements;
			if (currentElements == null) {
				return;
			}
			final Object[] eventElements = ModelEventService.getEventItems(event);
			AsyncDataViewerInput.this.handleEvent(currentElements, eventElements);
		}
	}
	
	protected void handleEvent(final List<TEntity> currentElements, final Object[] eventElements) {
		final List<TEntity> reloadedElements = loadElements();
		
		final Set<TEntity> changedElements = new HashSet<>();
		final Set<TEntity> removedElements = new HashSet<>();
		
		checkChangedElements(eventElements, (element) -> {
			if (element != null) {
				if (reloadedElements.contains(element)) {
					changedElements.add(element);
				}
				else if (currentElements.contains(element)) {
					removedElements.add(element);
				}
			}
		});
		
		if (!changedElements.isEmpty() || !removedElements.isEmpty()) {
			setElements(reloadedElements);
			final AsyncDataLoader<TEntity> dataLoader = this.dataLoader;
			if (dataLoader != null) {
				this.dataLoader.syncUpdate(reloadedElements, changedElements, removedElements);
			}
			Display.getDefault().asyncExec(this::updateInput);
		}
	}
	
	protected void checkChangedElements(final Object[] eventElements, final Consumer<TEntity> task) {
		if (eventElements.length > 0) {
			if (this.baseElementType.isInstance(eventElements[0])) {
				for (final Object eventElement : eventElements) {
					task.accept((TEntity)eventElement);
				}
			}
		}
	}
	
	@Override
	public void onDataLoaded(final List<? extends TEntity> elements, final boolean completed) {
		if (completed) { // update sorting
			final StructuredViewer viewer = getViewer();
			if (viewer == null || viewer.getControl().isDisposed()) {
				return;
			}
			if (viewer.getComparator() != null) {
				viewer.refresh();
				return;
			}
		}
		refreshViewer(elements);
	}
	
	
	protected void updateInput() {
		super.doSetValue(getBaseElements());
		
		final StructuredViewer viewer = getViewer();
		if (viewer == null || viewer.getControl().isDisposed()) {
			return;
		}
		viewer.setInput(getViewerElements());
	}
	
	public void refreshViewer() {
		final StructuredViewer viewer = getViewer();
		if (viewer == null || viewer.getControl().isDisposed()) {
			return;
		}
		viewer.refresh();
	}
	
	public void refreshViewer(final List<? extends TEntity> elements) {
		final StructuredViewer viewer = getViewer();
		if (viewer == null || viewer.getControl().isDisposed()) {
			return;
		}
		for (final TEntity element : elements) {
			viewer.refresh(element);
		}
	}
	
}
