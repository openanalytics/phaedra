package eu.openanalytics.phaedra.datacapture.scanner.queue;

import eu.openanalytics.phaedra.base.seda.IStageEventHandler;
import eu.openanalytics.phaedra.base.seda.StageEvent;
import eu.openanalytics.phaedra.base.seda.StageService;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.datacapture.scanner.Activator;
import eu.openanalytics.phaedra.datacapture.scanner.ScannerService;
import eu.openanalytics.phaedra.datacapture.scanner.model.ScanJob;

public class ScanJobQueue implements IStageEventHandler {

	private final static String ID = "datacapture.ScanJobQueue";
	
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
		ScanJob scanner = (ScanJob)event.data;
		ScannerService.getInstance().executeScanner(scanner, null);
	}

	@Override
	public void onEventException(StageEvent event, Throwable exception) {
		ScanJob scanner = (ScanJob)event.data;
		EclipseLog.error("Failed to execute data capture scanner '" + scanner.getLabel() + "'", exception, Activator.getDefault());		
	}
	
	/**
	 * Submit a scan job to the queue.
	 * Only the ScannerService should call this method.
	 * 
	 * @param scanJob The scan job to submit.
	 * @return True if the submission was accepted.
	 */
	public static boolean submit(ScanJob scanJob) {
		StageEvent event = new StageEvent();
		event.data = scanJob;
		event.targetStageId = ID;
		return StageService.getInstance().post(event);
	}
}
