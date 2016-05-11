package eu.openanalytics.phaedra.base.util.threading;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.eclipse.core.runtime.IProgressMonitor;

import eu.openanalytics.phaedra.base.util.misc.NumberUtils;
import eu.openanalytics.phaedra.base.util.threading.MTFactory.MTCallable;

public class MTExecutor<T> implements IConcurrentExecutor<T> {

	private int threadCount;
	private ThreadPool threadPool;
	
	private volatile boolean exceptionOccured;
	private volatile Exception lastExceptionCaught;
	
	private volatile long registeredTotalDuration;
	private volatile int registeredCompletions;
	
	private List<MTCallable<T>> queuedTasks;
	
	public MTExecutor() {
		// Default constructor
	}
	
	@Override
	public void init(int threadCount) {
		
		if (threadCount < 1) threadCount = 1;
		this.threadCount = threadCount;
		threadPool = new ThreadPool(threadCount);
		
		exceptionOccured = false;
		lastExceptionCaught = null;
		
		registeredCompletions = 0;
		registeredTotalDuration = 0;
		
		queuedTasks = new ArrayList<MTCallable<T>>();
	}
	
	@Override
	public void queue(Callable<T> task) {
		if (task instanceof MTCallable) {
			queuedTasks.add((MTCallable<T>)task);
		} else if (task instanceof Callable) {
			MTCallable<T> mtTask = new MTCallable<>();
			mtTask.setDelegate(task);
			queuedTasks.add(mtTask);
		} else {
			throw new IllegalArgumentException("An MTExecutor requires Callable or MTCallable tasks.");
		}
	}

	@Override
	public List<T> run(IProgressMonitor monitor) {
		
		List<Future<T>> results = new ArrayList<Future<T>>();
		List<T> output = new ArrayList<T>();
		
		int queueSize = queuedTasks.size();
		
		// Prepare all tasks for execution.
		int totalWorkload = 0;
		for (MTCallable<T> task: queuedTasks) {
			totalWorkload += task.getWorkload();
			task.setExecutor(this);
			task.setMonitor(monitor);
		}

		// Then, submit the rest. But only if manual trigger is disabled.
		for (int i=0; i<queuedTasks.size(); i++) {
			MTCallable<T> task = queuedTasks.get(i);
			results.add(threadPool.schedule(task));
		}
		
		if (monitor != null) {
			monitor.beginTask("Executing tasks", totalWorkload);
		}

		// Sleep, and periodically check the ThreadPool's status.
		int workload = queueSize;
		while (workload > 0) {
			
			// If aborted or interrupted, return immediately.
			if (Thread.interrupted() || (monitor != null && monitor.isCanceled())) {
				threadPool.stop(false);
				return output;
			}
			
			// Check for errors.
			checkForExceptions();
			
			// Update the monitor with progress info.
			workload = queueSize - (int)threadPool.getCompletedCount();

			String remaining = "";
			long avgDuration = getAvgDuration();
			long remainingMS = (avgDuration*workload)/threadCount;
			if (avgDuration != -1) remaining = " Time left: " + NumberUtils.formatDuration(remainingMS);
			if (monitor != null) {
				monitor.subTask("Working: " + workload + " item(s) remaining." + remaining);
			}

			// Sleep for a while.
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// The executor thread was interrupted. Abort immediately.
				threadPool.stop(false);
				return output;
			}
		}

		checkForExceptions();
		
		// At this point, the threadpool should be idle,
		// now shut it down to release the threads.
		threadPool.stop(false);
		
		try {
			for (Future<T> result: results) {
				output.add(result.get());	
			}
		} catch (Exception e) {
			throw new RuntimeException("Execution failed", e);
		}
		return output;
	}

	@Override
	public long getAvgDuration() {
		if (registeredCompletions == 0) return -1;
		return registeredTotalDuration/registeredCompletions;
	}
	
	/*
	 * Non-public
	 * **********
	 */
	
	protected void addPerformanceMetric(long duration) {
		registeredCompletions++;
		registeredTotalDuration += duration;
	}

	protected void handleException(Exception e) {
		exceptionOccured = true;
		lastExceptionCaught = e;
	}

	private void checkForExceptions() {
		if (exceptionOccured) {
			threadPool.stop(false);
			// One of the tasks threw an exception.
			if (lastExceptionCaught != null) {
				throw new RuntimeException(lastExceptionCaught);
			} else {
				throw new RuntimeException(
						"An unspecified error occured during execution");
			}
		}
	}
}
