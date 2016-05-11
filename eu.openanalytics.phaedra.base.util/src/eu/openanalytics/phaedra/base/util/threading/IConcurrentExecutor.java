package eu.openanalytics.phaedra.base.util.threading;

import java.util.List;
import java.util.concurrent.Callable;

import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Convenience class that represents a multithreaded executor service.
 * The flow of execution is:
 * <ol>
 * <li>Initialize the executor with a number of threads</li>
 * <li>Queue a number of tasks</li>
 * <li>Run the executor, while getting progress feedback</li>
 * <li>After execution, the executor should <b>not</b> be reused</li>
 * </ol>
 */
public interface IConcurrentExecutor<T> {

	/**
	 * Initialize the executor with the specified number of threads.
	 */
	public void init(int threadCount);
	
	/**
	 * Add a task to the queue.
	 * The task will not be executed until run() is called.
	 */
	public void queue(Callable<T> task);

	/**
	 * Launch all tasks in the queue.
	 * This method will block until all tasks are done, or one of them throws an exception.
	 * <p>
	 * Calling the queue() method after calling run() will result in unpredictable behaviour.
	 * </p>
	 */
	public List<T> run(IProgressMonitor monitor);

	/**
	 * Get an estimated duration (in milliseconds) per task.
	 * If no estimation can be made, -1 is returned.
	 */
	public long getAvgDuration();
	
}