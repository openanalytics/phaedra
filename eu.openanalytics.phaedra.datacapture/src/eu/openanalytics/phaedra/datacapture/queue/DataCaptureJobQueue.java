package eu.openanalytics.phaedra.datacapture.queue;

import eu.openanalytics.phaedra.base.seda.IStage;
import eu.openanalytics.phaedra.base.seda.IStageEventHandler;
import eu.openanalytics.phaedra.base.seda.StageConfiguration;
import eu.openanalytics.phaedra.base.seda.StageEvent;
import eu.openanalytics.phaedra.base.seda.StageService;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.datacapture.Activator;
import eu.openanalytics.phaedra.datacapture.DataCaptureService;
import eu.openanalytics.phaedra.datacapture.DataCaptureTask;
import eu.openanalytics.phaedra.datacapture.log.DataCaptureLogItem;
import eu.openanalytics.phaedra.datacapture.log.DataCaptureLogItem.LogItemSeverity;

public class DataCaptureJobQueue implements IStageEventHandler {

	private static final String ID = "datacapture.DataCaptureJobQueue";
	
	private static final String WORKER_POOL_SIZE = "phaedra.dc.workers";
	
	private static int workerPoolSize = 0;
	
	@Override
	public void customizeConfig(StageConfiguration config) {
		workerPoolSize = Integer.valueOf(System.getProperty(WORKER_POOL_SIZE, "" + config.getInt("threads")));
		config.set("threads", workerPoolSize);
	}
	
	@Override
	public void onStartup() {
		// Do nothing.
	}

	@Override
	public void onShutdown(boolean forced) {
		// Do nothing.
	}

	@Override
	public void handleEvent(StageEvent event) throws Exception {
		DataCaptureTask task = (DataCaptureTask)event.data;
		// Wait a little bit before executing the next task
		long sleep = (long)(Math.random() * 10000);
		Thread.sleep(sleep);
		DataCaptureService.getInstance().executeTask(task, null);
	}

	@Override
	public void onEventException(StageEvent event, Throwable exception) {
		EclipseLog.error("Failed to excecute data capture task", exception, Activator.getDefault());
	}
	
	/**
	 * Submit a data capture task to the queue.
	 * Only the DataCaptureService should call this method.
	 * 
	 * @param task The task to submit.
	 * @return True if the submission was accepted.
	 */
	public static boolean submit(DataCaptureTask task) {
		StageEvent event = new StageEvent();
		event.data = task;
		event.targetStageId = ID;
		return StageService.getInstance().post(event);
	}
	
	/**
	 * Cancel a queued data capture task.
	 * Only the DataCaptureService should call this method.
	 * 
	 * @param taskId The id of the task to cancel.
	 * @return True if the task was found and canceled.
	 */
	public static boolean cancel(String taskId) {
		IStage stage = StageService.getInstance().getStage(ID);
		for (StageEvent event: stage.getQueuedEvents()) {
			DataCaptureTask task = (DataCaptureTask)event.data;
			if (task.getId().equals(taskId)) {
				boolean canceled = stage.cancel(event);
				DataCaptureLogItem logItem = new DataCaptureLogItem(LogItemSeverity.Cancelled, task, null, null, "Data capture cancelled", null);
				if (canceled) DataCaptureService.getInstance().fireLogEvent(logItem);
				return canceled;
			}
		}
		return false;
	}
	
	/**
	 * Obtain the number of tasks currently in the queue.
	 * 
	 * @return The nr of queued tasks.
	 */
	public static int getQueueSize() {
		IStage stage = StageService.getInstance().getStage(ID);
		return stage.getQueuedEvents().length;
	}
	
	/**
	 * Obtain the number of worker threads that are assigned to execute DataCapture tasks.
	 * 
	 * @return The nr of datacapture worker threads.
	 */
	public static int getWorkerPoolSize() {
		return workerPoolSize;
	}
}
