package eu.openanalytics.phaedra.base.scripting.r;

import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends Plugin {

	public static final String PLUGIN_ID = "eu.openanalytics.phaedra.base.scripting.r"; //$NON-NLS-1$

	private static Activator plugin;
	
	private RScriptEngine engine;
	
	public Activator() {
		// Default constructor.
	}

	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	public void stop(BundleContext context) throws Exception {
		if (engine != null) engine.shutdown();
		plugin = null;
		super.stop(context);
	}

	public static Activator getDefault() {
		return plugin;
	}

	/* package */ void setRScriptEngine(RScriptEngine engine) {
		this.engine = engine;
	}
}
