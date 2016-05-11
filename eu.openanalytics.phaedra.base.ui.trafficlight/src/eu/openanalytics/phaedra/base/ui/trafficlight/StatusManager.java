package eu.openanalytics.phaedra.base.ui.trafficlight;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.swt.widgets.Display;

public class StatusManager {

	private static StatusManager instance;

	private StatusLineManager statusLineManager;

	private IStatusChecker[] statusCheckers;
	private CheckerRunnable runnable;

	private StatusManager() {
		// Hidden constructor
		statusCheckers = loadStatusCheckers();
		runnable = new CheckerRunnable();
		statusLineManager = new StatusLineManager(statusCheckers);
	}

	public static StatusManager getInstance() {
		if (instance == null) {
			instance = new StatusManager();
			instance.run();
		}
		return instance;
	}

	public static IContributionItem getContributionItem() {
		return getInstance().getStatusbarItem();
	}

	private void run() {
		Thread thread = new Thread(runnable, "Traffic Status Checker");
		thread.start();
	}

	public void pause() {
		runnable.pause();
	}

	public void resume() {
		runnable.resume();
	}

	public void forcePoll(Class<? extends IStatusChecker> clazz) {
		for (IStatusChecker checker: statusCheckers) {
			if (clazz == null || clazz.isAssignableFrom(checker.getClass())) {
				doSafeCheck(checker, true);
			}
		}
	}

	/*
	 * Non-public
	 */

	private ContributionItem getStatusbarItem() {
		return statusLineManager;
	}

	private void doSafeCheck(IStatusChecker checker, boolean test) {
		try {
			TrafficStatus status = test ? checker.test() : checker.poll();
			handleStatus(checker, status);
		} catch (Throwable t) {
			TrafficStatus status = new TrafficStatus(TrafficStatus.DOWN,
					"Status check failed: " + t.getMessage());
			handleStatus(checker, status);
		}
	}

	private void handleStatus(final IStatusChecker checker, final TrafficStatus status) {
		// Called from the checker thread.
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				statusLineManager.updateStatus(checker, status);
			}
		});
	}

	private IStatusChecker[] loadStatusCheckers() {
		List<IStatusChecker> checkers = new ArrayList<IStatusChecker>();
		IConfigurationElement[] config = Platform.getExtensionRegistry()
				.getConfigurationElementsFor(IStatusChecker.EXT_PT_ID);
		for (IConfigurationElement el : config) {
			try {
				Object o = el.createExecutableExtension(IStatusChecker.ATTR_CLASS);
				if (o instanceof IStatusChecker) {
					IStatusChecker checker = (IStatusChecker)o;
					checkers.add(checker);
				}
			} catch (CoreException e) {
				// Invalid extension.
			}
		}
		return checkers.toArray(new IStatusChecker[checkers.size()]);
	}

	private class CheckerRunnable implements Runnable {

		private volatile boolean running;
		private volatile Thread executingThread;

		private long interval = 30000;

		public void resume() {
			if (running) return;
			running = true;
			executingThread.notifyAll();
		}

		public void pause() {
			running = false;
		}

		@Override
		public void run() {
			running = true;
			executingThread = Thread.currentThread();
			while (true) {
				while (running) {
					for (IStatusChecker checker: statusCheckers) {
						doSafeCheck(checker, false);
					}
					try {
						Thread.sleep(interval);
					} catch (InterruptedException e) {}
				}
				try {
					executingThread.wait();
				} catch (InterruptedException e) {}
			}
		}

	}
}
