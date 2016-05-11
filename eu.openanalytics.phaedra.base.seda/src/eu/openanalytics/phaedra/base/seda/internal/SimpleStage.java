package eu.openanalytics.phaedra.base.seda.internal;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import eu.openanalytics.phaedra.base.seda.Activator;
import eu.openanalytics.phaedra.base.seda.IStage;
import eu.openanalytics.phaedra.base.seda.IStageController;
import eu.openanalytics.phaedra.base.seda.IStageEventHandler;
import eu.openanalytics.phaedra.base.seda.StageConfiguration;
import eu.openanalytics.phaedra.base.seda.StageEvent;
import eu.openanalytics.phaedra.base.seda.StageInternals;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;

public class SimpleStage implements IStage {

	private String id;
	
	private IStageEventHandler handler;
	private StageInternals internals;
	
	private SimpleStageRunner stageRunner;
	private Map<String, IStageController> controllers;
	
	public SimpleStage(String id) {
		this.id = id;
	}
	
	@Override
	public void configure(IStageEventHandler handler, StageConfiguration config) {
		int threads = config.getInt("threads");
		
		this.handler = handler;
		internals = new StageInternals(threads);
		stageRunner = new SimpleStageRunner();
		controllers = new ConcurrentHashMap<>();
	}
	
	@Override
	public void startup() {
		// Ignore duplicate startup() calls.
		if (stageRunner.isRunning()) return;
		
		internals.startThreadPool();
		new Thread(stageRunner, "StageRunner." + id).start();
		handler.onStartup();
		for (IStageController c: getControllers()) c.stageStarted();
	}

	@Override
	public void shutdown(boolean force) {
		// Ignore duplicate shutdown() calls.
		if (!stageRunner.isRunning()) return;
		
		stageRunner.stop();
		internals.stopThreadPool(force);
		handler.onShutdown(force);
		for (IStageController c: getControllers()) c.stageStopped();
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public void addController(IStageController controller) {
		controller.initialize(this, internals);
		controllers.put(controller.getId(), controller);
	}

	@Override
	public boolean submit(StageEvent event) {
		// Refuse all events if the stage is not running.
		if (!stageRunner.isRunning()) return false;
		
		boolean accepted = internals.getQueue().offer(event);
		if (accepted) {
			for (IStageController c: getControllers()) c.eventSubmitted(event);
		}
		return accepted;
	}

	@Override
	public boolean cancel(StageEvent event) {
		Object[] runnables = internals.getThreadPool().getQueue().toArray();
		for (int i = 0; i < runnables.length; i++) {
			EventHandlerTask task = (EventHandlerTask)runnables[i];
			if (task.event.equals(event)) return internals.getThreadPool().remove(task);
		}
		return false;
	}
	
	@Override
	public StageEvent[] getQueuedEvents() {
		// Note: all the events should be on the threadpool's queue, not the stage queue.
		Object[] runnables = internals.getThreadPool().getQueue().toArray();
		StageEvent[] events = new StageEvent[runnables.length];
		for (int i = 0; i < runnables.length; i++) {
			events[i] = ((EventHandlerTask)runnables[i]).event;
		}
		return events;
	}
	
	@Override
	public boolean isRunning() {
		return stageRunner.isRunning();
	}
	
	/*
	 * Non-public
	 * **********
	 */

	private void processEvent(StageEvent event) {
		for (IStageController c: getControllers()) c.preHandleEvent(event);
		try {
			handler.handleEvent(event);
			for (IStageController c: getControllers()) c.postHandleEvent(event, null);
		} catch (InterruptedException e) {
			// I was interrupted: the stage runner is being shut down.
		} catch (Throwable t) {
			// The event handler threw an exception. Notify the handler and the controllers of this.
			handler.onEventException(event, t);
			for (IStageController c: getControllers()) c.postHandleEvent(event, t);
			// Hereafter, the exception is discarded.
		}
	}
	
	private IStageController[] getControllers() {
		IStageController[] controllerArray = new IStageController[controllers.size()];
		return controllers.values().toArray(controllerArray);
	}
	
	private class SimpleStageRunner implements Runnable {

		private volatile boolean running;
		private volatile Thread hostThread;
		
		public SimpleStageRunner() {
			this.running = false;
		}
		
		public void stop() {
			if (!running) return;
			running = false;
			if (hostThread != null) hostThread.interrupt();
		}
		
		public boolean isRunning() {
			return running;
		}
		
		@Override
		public void run() {
			hostThread = Thread.currentThread();
			running = true;
			while (running) {
				try {
					// A new event is available. Submit it to the threadpool.
					StageEvent newEvent = internals.getQueue().take();
					EventHandlerTask task = new EventHandlerTask(newEvent);
					if (internals.getThreadPool() != null) {
						// There is a small chance threadpool is null at this point:
						// If a shutdown is in progress and a thread interrupt is not (yet) received.
						// In that case, ignore the task (it would be rejected anyway).
						internals.getThreadPool().execute(task);
					}
				} catch (InterruptedException e) {
					// I was interrupted, probably by the stop() method. Proceed to check the running variable.
				} catch (Throwable t) {
					// Recover from any error: do not allow the stage runner to crash.
					String msg = "Error in stage " + id + " while processing an event: " + t.getMessage();
					EclipseLog.error(msg, t, Activator.getDefault());
				}
			}
		}
	}
	
	private class EventHandlerTask implements Runnable {
		
		private StageEvent event;
		
		public EventHandlerTask(StageEvent event) {
			this.event = event;
		}
		
		@Override
		public void run() {
			processEvent(event);
		}
	}
}
