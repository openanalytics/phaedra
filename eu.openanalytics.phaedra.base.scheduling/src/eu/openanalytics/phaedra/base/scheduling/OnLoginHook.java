package eu.openanalytics.phaedra.base.scheduling;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.quartz.SchedulerException;

import eu.openanalytics.phaedra.base.hook.IHook;
import eu.openanalytics.phaedra.base.hook.IHookArguments;
import eu.openanalytics.phaedra.base.hook.PreHookException;

public class OnLoginHook implements IHook {

	@Override
	public void pre(IHookArguments args) throws PreHookException {
		// Do nothing.
	}

	@Override
	public void post(IHookArguments args) {
		try {
			SchedulingService.getInstance().initialize();
			SchedulingService.getInstance().getScheduler().start();
			scheduleRegisteredJobs();			
		} catch (SchedulerException e) {
			Activator.getDefault().getLog().log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Failed to initialize scheduler", e));
		}
	}

	private void scheduleRegisteredJobs() {
		IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(IScheduler.EXT_PT_ID);
		for (IConfigurationElement el : config) {			
			String elementType = el.getAttribute(IScheduler.ATTR_ID);
			try {
				Object o = el.createExecutableExtension(IScheduler.ATTR_CLASS);
				if (o instanceof IScheduler) {
					IScheduler iScheduler = (IScheduler) o;
					SchedulingService.getInstance().getScheduler().scheduleJobs(iScheduler.getJobsToTrigger(), false);
				} 
			} catch (CoreException e) {
				throw new IllegalArgumentException("Invalid scheduler: " + elementType);
			} catch (SchedulerException e) {
				throw new RuntimeException("failed to start scheduler " + elementType, e);
			}
		}		
	}
}
