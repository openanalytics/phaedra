package eu.openanalytics.phaedra.app.headless;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;

public class Activator extends Plugin {
	
	private static final String PROPERTY_FILE = "headless.properties";
	
	private Properties headlessProperties;
	
	// The shared instance
	private static Activator plugin;
	private static BundleContext context;
 
	/**
	 * The constructor
	 */
	public Activator() {
	}
		
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
		plugin = this;
		context = bundleContext;
		this.headlessProperties = loadProperties(PROPERTY_FILE);
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext bundleContext) throws Exception {
		plugin = null;
		context = null;
		this.headlessProperties = null;		
	}
	
	public Properties getHeadlessProperties() {
		return headlessProperties;
	}

	private static Properties loadProperties(String fileName) {
		Properties configFile = new Properties();
		InputStream configInput = null;
		
		try {
			File customConfigFile = new File(fileName);
			if (customConfigFile.exists()) {
				// A custom config file is present in the working directory, use it.
				configInput = new FileInputStream(customConfigFile);
			} else {
				// No custom config, use the defaults.
				configInput = FileLocator.openStream(context.getBundle(), new Path(fileName), false);
			}
			configFile.load(configInput);
			return configFile;
		} catch (Throwable t) {
			throw new RuntimeException("failed to load properties", t);
		} finally {
			if (configInput != null) {
				try { configInput.close(); } catch (IOException e) {}
			}
		}		
	}	
}
