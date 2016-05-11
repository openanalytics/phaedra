package eu.openanalytics.phaedra.base.seda;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import eu.openanalytics.phaedra.base.util.threading.ThreadPool;

public class StageInternals {

	private int threads;
	
	private BlockingQueue<StageEvent> queue;
	private ThreadPool threadPool;
	
	public StageInternals(int threads) {
		this.queue = new LinkedBlockingQueue<>();
		this.threads = Math.max(1, threads);
	}
	
	public void startThreadPool() {
		// There can be only one Threadpool active. 
		if (threadPool != null) return;
		threadPool = new ThreadPool(threads);		
	}
	
	public void stopThreadPool(boolean force) {
		if (threadPool == null) return;
		threadPool.stop(!force);
		threadPool = null;
	}
	
	public BlockingQueue<StageEvent> getQueue() {
		return queue;
	}
	
	public ThreadPool getThreadPool() {
		return threadPool;
	}
}
