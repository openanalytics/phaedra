package eu.openanalytics.phaedra.app.headless.updater;

import java.util.Properties;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.ui.IStartup;

import eu.openanalytics.phaedra.app.headless.Activator;
import eu.openanalytics.phaedra.app.headless.Application;

public class Updater implements IStartup{

	@Override
	public void earlyStartup() {
		Properties configFile = Activator.getDefault().getHeadlessProperties();
		int interval = Integer.parseInt(configFile.getProperty("interval"));
		if (interval == -1) return;
		
		IJobChangeListener provisionListener = new JobChangeAdapter(){
				@Override
				public void done(IJobChangeEvent event) {
					// Updates are done, shutdown application
					Application.shutdown();
				}
		};
		
		ScheduledThreadPoolExecutor scheduler = new ScheduledThreadPoolExecutor(1);
		
		//Do updates and shutdown when the job is completed
		//Relies on an external wrapper to restart PHAEDRA 
		scheduler.scheduleWithFixedDelay(
				new P2UpdateRun(provisionListener)
				, 0
				, interval
				, TimeUnit.valueOf(configFile.getProperty("unit"))
		);
	}
}
