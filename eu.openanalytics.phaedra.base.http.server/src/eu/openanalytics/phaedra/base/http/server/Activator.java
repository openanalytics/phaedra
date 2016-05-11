package eu.openanalytics.phaedra.base.http.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

public class Activator extends AbstractUIPlugin {
	
	public static final String PLUGIN_ID = Activator.class.getPackage().getName();

	public static final String HTTP_ENABLED = "http.server";
	
	private static Activator plugin;
	
	private List<IHttpService> httpServices = Collections.synchronizedList(new ArrayList<IHttpService>());
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		
		if (isEnabled()) {
			startHttpService();
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		if (isEnabled()) {
			stopHttpService();
		}
		
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}

	public boolean isEnabled() {
		return Boolean.parseBoolean(System.getProperty(HTTP_ENABLED, "false"));
	}
	
	private void startHttpService() {
		IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(IHttpService.EXT_PT_ID);
		for (IConfigurationElement el : config) {			
			String elementType = el.getAttribute(IHttpService.ATTR_ID);
			try {
				Object o = el.createExecutableExtension(IHttpService.ATTR_CLASS);
				if (o instanceof IHttpService) {					
					IHttpService httpService = (IHttpService) o;
					httpServices.add(httpService);					
					httpService.startup();
				} 
			} catch (CoreException e) {
				throw new IllegalArgumentException("Invalid Http Service: " + elementType);
			}
		}		
	}
	
	private void stopHttpService() {
		for (IHttpService httpService : httpServices) {
			httpService.shutdown();			
		}
		httpServices.clear();
	}
}
