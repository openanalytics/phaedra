package eu.openanalytics.phaedra.base.util.threading;

import java.util.concurrent.Callable;


public interface IConcurrentFactory<T> {
	
	public IConcurrentExecutor<T> createExecutor();
	
	public Callable<T> createCallable(Callable<T> delegate, IConcurrentExecutor<T> executor);
}
