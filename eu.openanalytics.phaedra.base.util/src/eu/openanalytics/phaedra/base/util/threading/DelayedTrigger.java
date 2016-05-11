package eu.openanalytics.phaedra.base.util.threading;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;

/**
 * <p>
 * Utility class to schedule a Runnable, but allow the schedule to "reset".
 * This means that a subsequent call to schedule will cancel the previous schedule.
 * </p>
 * <p>
 * This is most useful for delayed actions following a user trigger, e.g. listeners on a text field
 * that should only react when the user is done typing.
 * </p>
 */
public class DelayedTrigger {
	
	private Job runnableChecker;
	
	private Runnable runnable;
	
	private boolean runInDisplayThread;
	
	public DelayedTrigger() {
		runnableChecker = new Job("Delayed action") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				if (runnable != null) runNow();
				return Status.OK_STATUS;
			}
		};
		runnableChecker.setSystem(true);
	}
	
	public void schedule(int delay, boolean runInDisplayThread, Runnable runnable) {
		if (runnable == null) throw new IllegalArgumentException("No runnable provided");
		this.runInDisplayThread = runInDisplayThread;
		this.runnable = runnable;

		runnableChecker.cancel();
		runnableChecker.schedule(delay);
	}
	
	private void runNow() {
		try {
			if (runInDisplayThread) {
				Display.getDefault().syncExec(runnable);
			} else {
				runnable.run();
			}
		} catch (Throwable t) {
			// Ignore failed runnables. It is too late to report back to the user.
		}
	}
}
