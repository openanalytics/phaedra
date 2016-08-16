package eu.openanalytics.phaedra.calculation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Display;

import eu.openanalytics.phaedra.base.ui.util.misc.RefreshManager;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.base.util.threading.JobUtils;
import eu.openanalytics.phaedra.base.util.threading.ThreadUtils;
import eu.openanalytics.phaedra.calculation.pref.Prefs;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;

/**
 * <p>
 * Accessor for feature values of arbitrary wells.
 * For access on a per-plate basis, use {@link PlateDataAccessor} instead.
 * </p><p>
 * This accessor may return null instead of actual feature values, and will attempt to 'batch' data loading,
 * and is therefore only useful in UI components which expect to retrieve a large number of values from many wells.
 * </p>
 * <ol>
 * <li>Calling {@link #getObjectValue(Well, Feature)} may return a cached value, or may return null.</li>
 * <li>In the background, a queue is filled with data load requests</li>
 * <li>At a specific time (either a delay since the previous load, or when the queue reaches a max size), the batch load is executed</li>
 * <li>If a refresher is set (via {@link #setRefresher(Runnable)}), it will be called so that client code can respond to the availability of new data</li>
 * </ol>
 */
public class AsyncWellDataAccessor {

	private Set<Feature> featureQueue;
	private Set<Well> wellQueue;

	private AtomicInteger threadId;

	private RefreshManager refreshManager;
	private Runnable refresher;

	private int maxWellQueueSize;
	private int maxFeatureQueueSize;
	private int maxTotalQuerySize;

	public AsyncWellDataAccessor() {
		this.featureQueue = Collections.synchronizedSet(new HashSet<>());
		this.wellQueue = Collections.synchronizedSet(new HashSet<>());

		this.threadId = new AtomicInteger(0);

		int refreshDelay = Activator.getDefault().getPreferenceStore().getInt(Prefs.WDA_REFRESH_DELAY);
		this.maxWellQueueSize = Activator.getDefault().getPreferenceStore().getInt(Prefs.WDA_MAX_WELL_QUEUE);
		this.maxFeatureQueueSize = Activator.getDefault().getPreferenceStore().getInt(Prefs.WDA_MAX_FEATURE_QUEUE);
		this.maxTotalQuerySize = Activator.getDefault().getPreferenceStore().getInt(Prefs.WDA_MAX_QUERY_ITEMS);

		this.refresher = () -> { };
		this.refreshManager = new RefreshManager(false, refreshDelay, () -> emptyQueue());

		this.refreshManager.start();
	}

	public void setRefresher(Runnable refresher) {
		this.refresher = refresher;
	}

	public void dispose() {
		this.refreshManager.stop();
	}

	public Object getObjectValue(Well well, Feature feature) {
		if (!WellDataAccessor.isFeatureValueCached(well, feature)) {
			queueFeatureValue(well, feature);
			return null;
		}

		PlateDataAccessor accessor = CalculationService.getInstance().getAccessor(well.getPlate());
		if (feature.isNumeric()) {
			return accessor.getNumericValue(well, feature, feature.getNormalization());
		} else {
			return accessor.getStringValue(well, feature);
		}
	}

	private void queueFeatureValue(Well well, Feature feature) {
		featureQueue.add(feature);
		wellQueue.add(well);
		int featureQueueSize = featureQueue.size();
		int wellQueueSize = wellQueue.size();
		if (featureQueueSize > maxFeatureQueueSize || wellQueueSize > maxWellQueueSize || (featureQueueSize * wellQueueSize) > maxTotalQuerySize) {
			emptyQueue();
		} else {
			refreshManager.requestRefresh();
		}
	}

	private void emptyQueue() {
		if (wellQueue.isEmpty() && featureQueue.isEmpty()) return;

		List<Well> wells = new ArrayList<>(wellQueue);
		wellQueue.clear();
		List<Feature> features = new ArrayList<>(featureQueue);
		featureQueue.clear();

		int id = threadId.getAndIncrement();
		if (id >= 3) threadId.set(0);
		JobUtils.runBackgroundJob(monitor -> {
			if (monitor.isCanceled()) return;
			ThreadUtils.runQuery(() -> {
				if (monitor.isCanceled()) return;
				WellDataAccessor.fetchFeatureValues(wells, features, false, monitor);
			});
			if (monitor.isCanceled()) return;
			refresher.run();
		}, toString() + id, null);
	}

	public void preload(List<Well> wells, List<Feature> features) {
		if (wells.isEmpty() || features.isEmpty()) return;
		boolean isPreload = Activator.getDefault().getPreferenceStore().getBoolean(Prefs.WDA_PRELOAD);
		if (!isPreload) return;
		Display.getDefault().asyncExec(() -> {
			try {
				new ProgressMonitorDialog(Display.getDefault().getActiveShell()).run(true, true, new FeatureValuePreLoader(wells, features));
			} catch (Exception e) {
				EclipseLog.error(e.getMessage(), e, Activator.getDefault());
			}
		});
	}

	private class FeatureValuePreLoader implements IRunnableWithProgress {

		private List<Well> wells;
		private List<Feature> features;

		public FeatureValuePreLoader(List<Well> wells, List<Feature> features) {
			this.wells = wells;
			this.features = features;
		}

		@Override
		public void run(IProgressMonitor monitor) {
			SubMonitor subMonitor = SubMonitor.convert(monitor, "Loading Well Feature Values", 100);
			try {
				int nrofWells = wells.size();
				if (nrofWells == 1) {
					WellDataAccessor.fetchFeatureValues(wells.get(0), features, true);
				} else {
					WellDataAccessor.fetchFeatureValues(wells, features, true, subMonitor.split(90));
				}
			} catch (Exception e) {
				EclipseLog.error(e.getMessage(), e, Activator.getDefault());
			} finally {
				subMonitor.done();
			}
		}

	}

}
