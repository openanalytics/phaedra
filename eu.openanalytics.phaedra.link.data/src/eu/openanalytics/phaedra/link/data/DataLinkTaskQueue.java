package eu.openanalytics.phaedra.link.data;

import eu.openanalytics.phaedra.base.seda.IStageEventHandler;
import eu.openanalytics.phaedra.base.seda.StageEvent;
import eu.openanalytics.phaedra.base.seda.StageService;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;

public class DataLinkTaskQueue implements IStageEventHandler {

	private final static String ID = "datalink.DataLinkTaskQueue";
	
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
		DataLinkTask task = (DataLinkTask)event.data;
		DataLinkService.getInstance().executeTask(task, null);
	}

	@Override
	public void onEventException(StageEvent event, Throwable exception) {
		EclipseLog.error("Failed to excecute data link task", exception, Activator.getDefault());
	}
	
	/**
	 * Submit a data link task to the queue.
	 * Only the DataLinkService should call this method.
	 * 
	 * @param task The task to submit.
	 * @return True if the submission was accepted.
	 */
	public static boolean submit(DataLinkTask task) {
		StageEvent event = new StageEvent();
		event.data = task;
		event.targetStageId = ID;
		return StageService.getInstance().post(event);
	}
}
