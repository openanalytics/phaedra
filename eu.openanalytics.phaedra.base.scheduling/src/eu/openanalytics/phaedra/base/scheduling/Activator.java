package eu.openanalytics.phaedra.base.scheduling;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.BundleContext;
import org.quartz.SchedulerException;


public class Activator extends Plugin {
	
	// The plug-in ID
	public static final String PLUGIN_ID = Activator.class.getPackage().getName();

	// The shared instance
	private static Activator plugin;
 
	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext bundleContext) throws Exception {
		super.start(bundleContext);
		plugin = this;
		
		try {
			SchedulingService.getInstance().initialize();
			SchedulingService.getInstance().getScheduler().start();
			startSchedulers();			
		} catch (SchedulerException e) {
			getLog().log(new Status(IStatus.ERROR, PLUGIN_ID, "Failed to initialize scheduler", e));
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext bundleContext) throws Exception {
		SchedulingService.getInstance().getScheduler().shutdown();
		plugin = null;
		super.stop(bundleContext);
	}

	private void startSchedulers() {
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
