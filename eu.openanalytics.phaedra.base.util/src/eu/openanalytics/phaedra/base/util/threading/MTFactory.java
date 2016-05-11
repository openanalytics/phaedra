package eu.openanalytics.phaedra.base.util.threading;

import java.util.concurrent.Callable;

import org.eclipse.core.runtime.IProgressMonitor;

public class MTFactory<T> implements IConcurrentFactory<T> {

	@Override
	public IConcurrentExecutor<T> createExecutor() {
		return new MTExecutor<T>();
	}
	
	@Override
	public Callable<T> createCallable(Callable<T> delegate, IConcurrentExecutor<T> executor) {
		MTCallable<T> callable = new MTCallable<T>();
		callable.setExecutor((MTExecutor<T>) executor);
		callable.setDelegate(delegate);
		return callable;
	}
	
	public static class MTCallable<T> implements Callable<T> {
		
		protected IProgressMonitor monitor;
		protected MTExecutor<T> executor;
		private int delay;
		private Callable<T> delegate;

		public int getWorkload() {
			return 1;
		}
		
		public void setDelay(int delay) {
			this.delay = delay;
		}
		
		public void setExecutor(MTExecutor<T> executor) {
			this.executor = executor;
		}
		
		public void setMonitor(IProgressMonitor monitor) {
			this.monitor = monitor;
		}
		
		public void setDelegate(Callable<T> internal) {
			this.delegate = internal;
		}

		@Override
		public T call() throws Exception {
			T outcome = null;
			if (delay > 0) {
				Thread.sleep(delay);
			}
			long startTime = System.currentTimeMillis();
			try {
				outcome = delegate.call();
				
				if (monitor != null) {
					monitor.worked(getWorkload());
				}
			} catch (Exception e) {
				executor.handleException(e);
			}
			long stopTime = System.currentTimeMillis();
			executor.addPerformanceMetric(stopTime - startTime);
			return outcome;
		}
	}
}
