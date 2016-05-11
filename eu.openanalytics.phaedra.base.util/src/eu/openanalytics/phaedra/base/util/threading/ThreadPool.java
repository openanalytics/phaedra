package eu.openanalytics.phaedra.base.util.threading;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Convenience wrapper around a ThreadPoolExecutor.
 */
public class ThreadPool {

	private ThreadPoolExecutor service;

	public ThreadPool(int size) {
		service = (ThreadPoolExecutor) Executors.newFixedThreadPool(size);
	}

	public void execute(Runnable task) {
		service.execute(task);
	}
	
	public Future<?> schedule(Runnable task) {
		return service.submit(task);
	}

	public <T> Future<T> schedule(Callable<T> task) {
		return service.submit(task);
	}
	
	public boolean isIdle() {
		if (service.getQueue().size() == 0 && service.getActiveCount() == 0)
			return true;
		return false;
	}

	public int getWorkload() {
		return service.getActiveCount() + service.getQueue().size(); 
	}
	
	public long getCompletedCount() {
		return service.getCompletedTaskCount();
	}
	
	public void setPoolSize(int size) {
		service.setCorePoolSize(size);
	}

	public void clearQueue() {
		service.getQueue().clear();
	}
	
	public BlockingQueue<Runnable> getQueue() {
		return service.getQueue();
	}
	
	public boolean remove(Runnable task) {
		return service.remove(task);
	}
	
	public void stop(boolean graceful) {
		if (graceful) {
			service.shutdown();			
		} else {
			service.shutdownNow();
		}
	}
}
