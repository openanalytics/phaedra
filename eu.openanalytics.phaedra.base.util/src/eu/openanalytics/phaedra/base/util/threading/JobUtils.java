package eu.openanalytics.phaedra.base.util.threading;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.operation.IRunnableWithProgress;

import eu.openanalytics.phaedra.base.util.Activator;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;

public class JobUtils {

	public static Job runUserJob(Runnable work, String title) {
		return runUserJob(monitor -> work.run(), title);
	}

	public static Job runUserJob(IRunnableWithProgress work, String title) {
		return runUserJob(work, title, 100, null, null);
	}

	public static Job runUserJob(IRunnableWithProgress work, String title, int totalWork, String uniqueId, String queueId) {
		return runJob(work, true, title, totalWork, uniqueId, queueId);
	}

	public static Job runBackgroundJob(Runnable work) {
		return runBackgroundJob(monitor -> work.run());
	}

	public static Job runBackgroundJob(IRunnableWithProgress work) {
		return runBackgroundJob(work, null, null);
	}

	public static Job runBackgroundJob(IRunnableWithProgress work, String uniqueId, String queueId) {
		return runJob(work, false, null, 100, uniqueId, queueId);
	}

	public static Job runBackgroundJob(IRunnableWithProgress work, String uniqueId, String queueId, long delay) {
		return runJob(work, false, null, 100, uniqueId, queueId, delay);
	}

	/**
	 * Schedule a Job in the Eclipse Job framework.
	 *
	 * @param work The work to execute in the job.
	 * @param user If true, the job is visible to the user (in the status bar and progress view).
	 * @param title The title for the job (optional).
	 * @param uniqueId If not null, this job will cancel other jobs with the same uniqueId before starting.
	 * @param queueId If not null, this job will wait until other jobs with the same queueId are done.
	 */
	public static Job runJob(IRunnableWithProgress work, boolean user, String title, int totalWork, String uniqueId, String queueId) {
		return runJob(work, user, title, totalWork, uniqueId, queueId, 0l);
	}

	/**
	 * Schedule a Job in the Eclipse Job framework.
	 *
	 * @param work The work to execute in the job.
	 * @param user If true, the job is visible to the user (in the status bar and progress view).
	 * @param title The title for the job (optional).
	 * @param uniqueId If not null, this job will cancel other jobs with the same uniqueId before starting.
	 * @param queueId If not null, this job will wait until other jobs with the same queueId are done.
	 * @param delay The delay in milliseconds for running this job, 0 for no delay.
	 */
	public static Job runJob(IRunnableWithProgress work, boolean user, String title, int totalWork, String uniqueId, String queueId, long delay) {
		final String titleToUse = (title == null) ? "Working" : title;

		Job job = new Job(titleToUse) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				monitor.beginTask(titleToUse, totalWork);
				try {
					work.run(monitor);
				} catch (Throwable t) {
					EclipseLog.error("Background job failed: " + t.getMessage(), t, Activator.getDefault());
				}
				monitor.done();
				return Status.OK_STATUS;
			};

			@Override
			public boolean belongsTo(Object family) {
				if (uniqueId != null) return uniqueId.equals(family);
				return super.belongsTo(family);
			}
		};

		if (queueId != null) job.setRule(new QueueIdJobRule(queueId));

		// Only one job of the family 'uniqueId' may be running at a given time.
		if (uniqueId != null) cancelJobs(uniqueId);

		job.setSystem(!user);
		job.schedule(delay);
		return job;
	}

	/**
	 * <p>Reschedule job if not <code>null</code>.</p>
	 *
	 * @param job
	 */
	public static void rescheduleJob(Job job) {
		rescheduleJob(job, 0l);
	}

	/**
	 * <p>Reschedule job if not <code>null</code>.</p>
	 *
	 * @param job
	 * @param delay a time delay in milliseconds before the job should run
	 */
	public static void rescheduleJob(Job job, long delay) {
		cancelJob(job);
		if (job != null) job.schedule(delay);
	}

	/**
	 * <p>Cancel jobs of the given family.</p>
	 *
	 * @param family
	 */
	public static void cancelJobs(Object family) {
		Job[] conflictingJobs = Job.getJobManager().find(family);
		for (Job job: conflictingJobs) cancelJob(job);
	}

	/**
	 * <p>Cancel given job if not <code>null</code>.</p>
	 *
	 * @param job
	 */
	public static void cancelJob(Job job) {
		if (job != null) job.cancel();
	}

	private static class QueueIdJobRule implements ISchedulingRule {

		private String queueId;

		public QueueIdJobRule(String queueId) {
			this.queueId = queueId;
		}

		@Override
		public boolean contains(ISchedulingRule rule) {
			return this == rule;
		}

		@Override
		public boolean isConflicting(ISchedulingRule rule) {
			if (rule instanceof QueueIdJobRule) {
				return ((QueueIdJobRule)rule).queueId.equals(this.queueId);
			}
			return false;
		}
	}
}
