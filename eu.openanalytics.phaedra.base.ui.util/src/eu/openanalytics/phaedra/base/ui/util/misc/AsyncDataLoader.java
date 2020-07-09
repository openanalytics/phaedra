package eu.openanalytics.phaedra.base.ui.util.misc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;

import eu.openanalytics.phaedra.base.util.Activator;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.base.util.threading.ThreadUtils;


/**
 * Helper loading and caching data for entities.
 * 
 * Loading of data is performed in the background using the ThreadUtils worker pool.
 *
 * @param <TEntity> the type of elements in the input
 */
public class AsyncDataLoader<TEntity> {
	
	
	public static final DataLoadStatus LOADING = new DataLoadStatus(IStatus.INFO, "\u23F3");
	public static final DataLoadStatus LOAD_ERROR = new DataLoadStatus(IStatus.ERROR, "<ERROR>");
	
	private static final Object[] NOT_YET_LOADED = new Object[0];
	
	
	/**
	 * Provides access to the requested data and allows to dispose the request.
	 * 
	 * @param <TData> the type of the data
	 */
	public class DataAccessor<TData> {
		
		private static final int NOT_AVAILABLE = Integer.MAX_VALUE;
		
		
		private final Function<TEntity, TData> loader;
		
		private int idx = NOT_AVAILABLE;
		
		
		private DataAccessor(final Function<TEntity, TData> loader) {
			this.loader = loader;
		}
		
		
		/**
		 * Returns the requested data value or a {@link DataLoadStatus status} if not available.
		 *
		 * Thread: display thread only.
		 * 
		 * @param element the entity
		 * @return the loaded data or a {@link DataLoadStatus} if the data is not available
		 */
		public Object getData(final TEntity element) {
			final AsyncDataLoader<TEntity>.LoadData data = AsyncDataLoader.this.currentData;
			final Object[] dataset;
			if ((dataset = data.dataMap.get(element)) == null || this.idx >= dataset.length) {
				requestLoading(element);
				return LOADING;
			}
			return dataset[this.idx];
		}
		
		/**
		 * Disposes the request.
		 * 
		 * Thread: display thread only.
		 */
		public void dispose() {
			this.idx = NOT_AVAILABLE;
			removeDataRequest(this);
		}
		
	}
	
	public static interface Listener<TEntity> {
		
		/**
		 * Called by the loader if the data of elements was loaded.
		 * 
		 * Thread: display thread only.
		 * 
		 * @param elements the elements updated since last call
		 * @param completed if the background job completed since last call
		 */
		void onDataLoaded(final List<? extends TEntity> elements, final boolean completed);
		
	}
	
	
	private final String label;
	
	private final List<DataAccessor<?>> accessors = new ArrayList<>();
	
	private final RefreshRunnable listenerRunnable;
	
	private final Consumer<Job> jobScheduler;
	private LoadData currentData = new LoadData(Collections.emptyList(), Collections.emptyList());
	private LoadDataJob currentDataJob;
	
	
	/**
	 * 
	 * @param label a label for the data to load
	 * @param refreshCallback a callback executed in the display thread or <code>null</code>
	 */
	public AsyncDataLoader(final String label, final Consumer<Job> jobScheduler) {
		this.label = label;
		this.listenerRunnable = new RefreshRunnable();
		this.jobScheduler = jobScheduler;
	}
	
	public AsyncDataLoader(final String label) {
		this(label, Job::schedule);
	}
	
	/**
	 * Disposes this data loader.
	 * 
	 * Thread: display thread only.
	 */
	public synchronized void dispose() {
		this.accessors.clear();
		
		final LoadDataJob dataJob = this.currentDataJob;
		if (dataJob != null) {
			this.currentDataJob = null;
			dataJob.cancel();
		}
	}
	
	
	public void addListener(final Listener<TEntity> listener) {
		this.listenerRunnable.listeners.add(listener);
	}
	
	public void removeListener(final Listener<TEntity> listener) {
		this.listenerRunnable.listeners.remove(listener);
	}
	
	
	/**
	 * Adds an additional data request to the loader.
	 * 
	 * Thread: display thread only.
	 * 
	 * @param <TValue>
	 * @param function the function to load the data value
	 * @return a DataAccess handler for this request
	 */
	public <TValue> DataAccessor<TValue> addDataRequest(final Function<TEntity, TValue> function) {
		final DataAccessor<TValue> accessor = new DataAccessor<>(function);
		this.accessors.add(accessor);
		return accessor;
	}
	
	private void removeDataRequest(final DataAccessor<?> accessor) {
		this.accessors.remove(accessor);
	}
	
	
	/**
	 * Starts loading the data for the specified elements.
	 * 
	 * Thread: display thread only.
	 * 
	 * @param elements all current elements
	 * @param properties changed properties or <code>null</code> to force reload of all data
	 */
	public void asyncLoad(final List<TEntity> elements, final Collection<String> properties) {
		if (elements == null) {
			throw new NullPointerException("elements");
		}
		async(elements, properties);
	}
	
	/**
	 * Starts (re)loading the data for the current elements.
	 * 
	 * Thread: display thread only.
	 * 
	 * @param force <code>true</code> to invalidate all loaded data
	 */
	public void asyncReload(final boolean force) {
		async(null, (force) ? null : Collections.emptySet());
	}
	
	private void async(List<TEntity> elements, final Collection<String> properties) {
		final boolean force = (properties == null || !properties.isEmpty()); // possible enhancement: ask request if reload is required
		final List<DataRequest> requests = new ArrayList<>(this.accessors.size());
		for (int idx = 0; idx < this.accessors.size(); idx++) {
			final DataAccessor<?> accessor = this.accessors.get(idx);
			final DataRequest request = new DataRequest(idx, accessor.idx, accessor.loader);
			requests.add(request);
			accessor.idx = idx;
		}
		
		synchronized (this) {
			{	final LoadDataJob dataJob = this.currentDataJob;
				if (dataJob != null) {
					this.currentDataJob = null;
					dataJob.cancel();
				}
				this.listenerRunnable.clear();
			}
			
			final LoadData previousData = this.currentData;
			if (elements == null) {
				elements = previousData.elements;
			}
			final LoadData data = new LoadData(requests, elements);
			for (final TEntity element : elements) {
				data.dataMap.put(element, NOT_YET_LOADED);
			}
			LoadDataJob dataJob = null;
			this.currentData = data;
			if (!elements.isEmpty() && !requests.isEmpty()) {
				dataJob = new LoadDataJob(data, (!force) ? previousData.dataMap : null);
				this.jobScheduler.accept(dataJob);
			}
			this.currentDataJob = dataJob;
		}
	}
	
	private void asyncCompleted(final LoadDataJob job) {
		synchronized (this) {
			if (this.currentDataJob != job) {
				return;
			}
			this.currentDataJob = null;
		}
		this.listenerRunnable.onJobCompleted();
	}
	
	
	/**
	 * Updates the specified elements directly in current thread.
	 * 
	 * Thread: any background thread.
	 * 
	 * @param elements all current elements
	 * @param changedElements the changed elements to reload
	 * @param removedElements the removed elements
	 */
	public void syncUpdate(final List<TEntity> elements, final Collection<TEntity> changedElements, final Collection<TEntity> removedElements) {
		final LoadData data;
		synchronized (this) {
			data = this.currentData;
			
			data.elements = elements;
		}
		for (final TEntity element : removedElements) {
			data.dataMap.remove(element);
		}
		if (!changedElements.isEmpty()) {
			ThreadUtils.runQuery(() -> {
				changedElements.parallelStream().forEach((element) -> {
					updateElement(element, data, true, null);
				});
			});
		}
	}
	
	
	private void requestLoading(final TEntity element) {
		// possible enhancement: add to queue / move to top of queue
	}
	
	private void updateElement(final TEntity element, final LoadData data,
			final boolean forceAdd, final Map<TEntity, Object[]> previousDataMap) {
		final Object[] values = new Object[data.requests.size()];
		
		final Object[] previousValues;
		if (previousDataMap != null && (previousValues = previousDataMap.get(element)) != null
				&& previousValues != NOT_YET_LOADED) {
			for (final DataRequest request : data.requests) {
				if (previousValues != null && request.previousIdx < previousValues.length) {
					values[request.idx] = previousValues[request.previousIdx];
				} else {
					values[request.idx] = request.loadData(element);
				}
			}
		}
		else {
			for (final DataRequest request : data.requests) {
				try {
					values[request.idx] = request.loadData(element);
				} catch (final Exception e) {
					values[request.idx] = LOAD_ERROR;
				}
			}
		}
		
		if (data.dataMap.put(element, values) == null && !forceAdd) { // removed
			data.dataMap.remove(element, values);
			return;
		}
		
		final RefreshRunnable refresh = this.listenerRunnable;
		if (refresh != null) {
			refresh.onElementUpdated(element);
		}
	}
	
	
	private class DataRequest {
		
		
		private final int idx;
		private final int previousIdx;
		
		private final Function<TEntity, ?> loader;
		
		
		public DataRequest(final int idx, final int previousIdx, final Function<TEntity, ?> loader) {
			this.idx = idx;
			this.previousIdx = previousIdx;
			this.loader = loader;
		}
		
		
		public Object loadData(final TEntity element) {
			try {
				return this.loader.apply(element);
			}
			catch (final Exception e) {
				EclipseLog.error(String.format("An error occured when loading viewer data for %1$s", element),
						e, Activator.getDefault() );
				return LOAD_ERROR;
			}
		}
		
	}
	
	private class LoadData {
		
		
		private final List<DataRequest> requests;
		
		private List<TEntity> elements;
		
		private final Map<TEntity, Object[]> dataMap;
		
		
		public LoadData(final List<DataRequest> requests, final List<TEntity> input) {
			this.requests = requests;
			this.elements = input;
			this.dataMap = new ConcurrentHashMap<>(input.size());
		}
		
	}
	
	private class LoadDataJob extends Job {
		
		
		private final List<TEntity> elements;
		private final LoadData loadData;
		private final Map<TEntity, Object[]> previousDataMap;
		
		
		public LoadDataJob(final LoadData currentData, final Map<TEntity, Object[]> previousDataMap) {
			super(String.format("Load %1$s", AsyncDataLoader.this.label));
			this.elements = currentData.elements;
			this.loadData = currentData;
			this.previousDataMap = previousDataMap;
		}
		
		@Override
		protected IStatus run(final IProgressMonitor monitor) {
			final int n = this.elements.size();
			final SubMonitor m = SubMonitor.convert(monitor, String.format("Loading %1$s", AsyncDataLoader.this.label), n);
			
			try {
				ThreadUtils.runQuery(() -> {
					this.elements.parallelStream().forEach((element) -> {
						if (m.isCanceled()) {
							return;
						}
						
						updateElement(element, this.loadData, false, this.previousDataMap);
						synchronized (m) {
							m.worked(1);
						}
					});
				});
				
				asyncCompleted(this);
			} finally {
				m.done();
			}
			
			if (m.isCanceled()) {
				return Status.CANCEL_STATUS;
			}
			
			return Status.OK_STATUS;
		}
	}
	
	private class RefreshRunnable implements Runnable {
		
		private final ListenerList<Listener<TEntity>> listeners = new ListenerList<>(ListenerList.IDENTITY);
		
		private final List<TEntity> elements = new ArrayList<>();
		private boolean completed;
		
		private boolean sheduled;
		
		public RefreshRunnable() {
		}
		
		public synchronized void clear() {
			this.elements.clear();
			this.completed = false;
		}
		
		public synchronized void onElementUpdated(final TEntity element) {
			this.elements.add(element);
			if (!this.sheduled) {
				this.sheduled = true;
				Display.getDefault().asyncExec(this);
			}
		}
		
		public synchronized void onJobCompleted() {
			this.completed = true;
			if (!this.sheduled) {
				this.sheduled = true;
				Display.getDefault().asyncExec(this);
			}
		}
		
		@Override
		public void run() {
			final List<TEntity> elements;
			final boolean completed;
			synchronized (this) {
				this.sheduled = false;
				if (this.elements.isEmpty() && !this.completed) {
					return;
				}
				completed = this.completed;
				elements = new ArrayList<>(this.elements);
				clear();
			}
			
			for (final Object listener : this.listeners.getListeners()) {
				((Listener<TEntity>)listener).onDataLoaded(elements, completed);
			}
		}
		
	}
	
}
