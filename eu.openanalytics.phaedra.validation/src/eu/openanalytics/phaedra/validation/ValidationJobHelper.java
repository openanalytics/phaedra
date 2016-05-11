package eu.openanalytics.phaedra.validation;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;

import eu.openanalytics.phaedra.validation.ValidationService.Action;

public class ValidationJobHelper {

	public static void doInJob(final Action action, final String remark, final Object... objects) {
		Job job = createJob(action, remark, objects);
		job.schedule();
	}
	
	public static void doInJob(final Action action, final String remark, final Runnable postOkRunnable, final Object... objects) {
		Job job = createJob(action, remark, objects);
		postJobComplete(job, true, postOkRunnable);
		job.schedule();
	}
	
	public static Job createJob(final Action action, final String remark, final Object... objects) {
		Job job = new Job("Changing status"){
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				monitor.beginTask("Changing status: " + action.name(), IProgressMonitor.UNKNOWN);
				ValidationService.getInstance().doAction(action, remark, true, objects);
				monitor.done();
				return Status.OK_STATUS;
			}
		};
		job.setUser(true);
		return job;
	}
	
	public static void postJobComplete(Job job, final boolean okOnly, final Runnable runnable) {
		job.addJobChangeListener(new JobChangeAdapter() {
			@Override
			public void done(IJobChangeEvent event) {
				if (!okOnly || event.getResult().isOK()) {
					runnable.run();
				}
			}
		});
	}
}
