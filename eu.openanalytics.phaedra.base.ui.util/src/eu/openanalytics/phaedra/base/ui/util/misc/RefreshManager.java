package eu.openanalytics.phaedra.base.ui.util.misc;

import org.eclipse.swt.widgets.Display;

/**
 * A utility class to prevent excessive amounts of refresh calls.
 * <p>
 * No matter how often <code>requestRefresh()</code> is called, the <code>refresher</code> Runnable is executed
 * no more than once per <code>refreshDelay</code>.
 * </p>
 * <p>
 * The <code>refresher</code> Runnable should contain something like <code>widget.refresh()</code> and will be executed
 * in the Display thread.
 * </p>
 */
public class RefreshManager implements Runnable {

	private boolean isPostDelay;
	private int refreshDelay;
	private Runnable refresher;

	private volatile boolean refreshRequested;
	private volatile boolean running;
	private volatile boolean shutdownRequested;

	private Object notifier = new Object();

	public RefreshManager(int refreshDelay, Runnable refresher) {
		this(true, refreshDelay, refresher);
	}

	/**
	 *
	 *
	 * @param isPostDelay <code>true</code> to wait after running or <code>false</code> to wait before running.
	 * @param refreshDelay
	 * @param refresher
	 */
	public RefreshManager(boolean isPostDelay, int refreshDelay, Runnable refresher) {
		this.isPostDelay = isPostDelay;
		this.refreshDelay = refreshDelay;
		this.refresher = refresher;
	}

	@Override
	public void run() {
		while (!shutdownRequested) {
			if (!refreshRequested) {
				synchronized (notifier) {
					try { notifier.wait(); } catch (InterruptedException e) {}
				}
			}
			if (refreshRequested) {
				if (!isPostDelay) {
					try { Thread.sleep(refreshDelay); } catch (InterruptedException e) {}
				}
				refreshRequested = false;
				Display.getDefault().asyncExec(() -> refresher.run());
				if (isPostDelay) {
					try { Thread.sleep(refreshDelay); } catch (InterruptedException e) {}
				}
			}
		}
		running = false;
	}

	public void requestRefresh() {
		if (!running || refreshRequested) return;
		this.refreshRequested = true;
		synchronized (notifier) { notifier.notifyAll(); }
	}

	public void start() {
		if (running) return;
		running = true;
		new Thread(this, "Refresh Manager").start();
	}

	public void stop() {
		shutdownRequested = true;
		synchronized (notifier) { notifier.notifyAll(); }
	}
}