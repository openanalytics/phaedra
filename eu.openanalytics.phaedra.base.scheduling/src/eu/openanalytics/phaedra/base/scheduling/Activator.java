package eu.openanalytics.phaedra.base.scheduling;

import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;


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
}
