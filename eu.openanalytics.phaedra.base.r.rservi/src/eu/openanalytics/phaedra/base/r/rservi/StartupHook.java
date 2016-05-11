package eu.openanalytics.phaedra.base.r.rservi;

import eu.openanalytics.phaedra.base.hook.BaseHook;
import eu.openanalytics.phaedra.base.hook.IHookArguments;
import eu.openanalytics.phaedra.base.ui.trafficlight.StatusManager;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;

public class StartupHook extends BaseHook {

	@Override
	public void post(IHookArguments args) {
		EclipseLog.info("Starting R engine", Activator.getDefault());
		RService.getInstance().initialize();
		
		try {
			StatusManager.getInstance().forcePoll(RStatusChecker.class);
		} catch(Exception e) {
			// Running in headless mode.
		}
		
		boolean isRunning = RService.getInstance().isRunning();
		if (isRunning) {
			EclipseLog.info("R engine up and running", Activator.getDefault());
		} else {
			EclipseLog.warn("R engine not available! Check the log file for error messages.", Activator.getDefault());
		}
	}

}
