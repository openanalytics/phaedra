package eu.openanalytics.phaedra.model.protocol.util;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;

import eu.openanalytics.phaedra.base.db.IValueObject;
import eu.openanalytics.phaedra.base.util.threading.ThreadUtils;

public abstract class AbstractSummaryLoader<ENTITY extends IValueObject, T> {

	private List<ENTITY> input;
	private Consumer<ENTITY> refreshCallback;
	private Map<IValueObject, T> summaries;
	private LoadSummariesJob loadSummariesJob;

	public AbstractSummaryLoader(List<ENTITY> input, Consumer<ENTITY> refreshCallback) {
		this.input = input;
		this.refreshCallback = refreshCallback;
		this.summaries = new ConcurrentHashMap<>();
		this.loadSummariesJob = new LoadSummariesJob();
	}

	public void start() {
		loadSummariesJob.schedule();
	}

	public void stop() {
		if (loadSummariesJob != null) loadSummariesJob.cancel();
	}

	public T getSummary(IValueObject item) {
		if (summaries.containsKey(item)) return summaries.get(item);
		else return getEmptySummary();
	}

	public void update(ENTITY item) {
		update(item, createSummary(item));
	}
	
	public void update(ENTITY item, T summary) {
		summaries.put(item, summary);
		refreshItem(item);
	}
	
	protected abstract T getEmptySummary();

	protected abstract T createSummary(ENTITY item);

	private void refreshItem(ENTITY item) {
		if (refreshCallback == null) return;
		Display.getDefault().asyncExec(() -> refreshCallback.accept(item));
	}

	private class LoadSummariesJob extends Job {

		public LoadSummariesJob() {
			super("Loading summaries");
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			monitor.beginTask("Loading summaries", input.size());

			try {
				ThreadUtils.runQuery(() -> {
					input.parallelStream().forEach(item -> {
						if (monitor.isCanceled()) return;

						T summary = createSummary(item);
						summaries.put(item, summary);
						refreshItem(item);
						monitor.worked(1);
					});
				});
			} finally {
				monitor.done();
			}

			if (monitor.isCanceled()) return Status.CANCEL_STATUS;

			return Status.OK_STATUS;
		}
	}

}
