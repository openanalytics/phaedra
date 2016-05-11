package eu.openanalytics.phaedra.base.util.threading;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import eu.openanalytics.phaedra.base.util.Activator;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;

public class ThreadUtils {

	private static ForkJoinPool DB_POOL;

	/**
	 * Block the current thread until the specified ThreadPoolExecutor is idle
	 * (no running tasks and no queued tasks).
	 *
	 * @param tp The ThreadPoolExecutor to wait for.
	 */
	public static void waitUntilIdle(ThreadPoolExecutor tp) {
		while (tp.getQueue().size() > 0 || tp.getActiveCount() > 0) {
			try { Thread.sleep(500); } catch (InterruptedException e) {}
		}
	}

	/**
	 * <p>Same as {@link #runQuery(Runnable, boolean)} with isBlocking <code>true</code></p>
	 *
	 * @param task The task to execute
	 */
	public static void runQuery(Runnable task) {
		runQuery(task, true);
	}

	/**
	 * <p>Executes a {@link Runnable} on a Thread Pool that has the same size as DB Connections.</p>
	 *
	 * <p>Should be used for {@link Runnable}'s that use a DB Connection.</p>
	 *
	 * <p>If the DB Pool was not initialized, it will execute them in the calling thread. (See {@link #startDBPool(int)})</p>
	 *
	 * @param task The task to execute
	 * @param isBlocking Make the call wait for the operation to finish
	 */
	public static void runQuery(Runnable task, boolean isBlocking) {
		if (DB_POOL == null) {
			EclipseLog.error("The DB Pool has not yet been initialised.", null, Activator.getDefault());
			task.run();
		} else {
			run(DB_POOL, task, isBlocking);
		}
	}

	/**
	 * <p>Starts the DB Pool. Should be called on launch.</p>
	 *
	 * <p>If the pool has already been initialized, it will do nothing.</p>
	 *
	 * @param nrOfThreads
	 */
	public static void startDBPool(int nrOfThreads) {
		if (DB_POOL != null) EclipseLog.warn("The DB Pool is already running.", Activator.getDefault());
		else DB_POOL = new ForkJoinPool(nrOfThreads, ForkJoinPool.defaultForkJoinWorkerThreadFactory, null, true);
	}

	/**
	 * <p>Change the number of threads for the DB Pool.</p>
	 *
	 * <p>Since {@link ForkJoinPool} does not allow changing the number of threads, the existing DB Pool will be closed.</p>
	 *
	 * @param nrOfThreads
	 */
	public static void configureDBThreadPool(int nrOfThreads) {
		if (DB_POOL == null) {
			EclipseLog.warn("No DB Pool was initialized. Initialize it.", null, Activator.getDefault());
			DB_POOL = new ForkJoinPool(nrOfThreads, ForkJoinPool.defaultForkJoinWorkerThreadFactory, null, true);
		} else {
			ForkJoinPool temp = DB_POOL;
			// Let the variable reference a new thread pool.
			DB_POOL = new ForkJoinPool(nrOfThreads);
			// Close the previous thread pool after it has finished running.
			temp.awaitQuiescence(10, TimeUnit.SECONDS);
			temp.shutdownNow();
		}
	}

	/**
	 * <p>Run a task on given thread pool.</p>
	 *
	 * <p>Error messages are logged.</p>
	 *
	 * @param executor
	 * @param task
	 */
	private static void run(ExecutorService executor, Runnable task, boolean isBlocking) {
		try {
			Future<?> submit = executor.submit(task);
			if (isBlocking) submit.get();
		} catch (InterruptedException e) {
			EclipseLog.error(e.getMessage(), e, Activator.getDefault());
		} catch (ExecutionException e) {
			EclipseLog.error(e.getMessage(), e, Activator.getDefault());
		}
	}

}
