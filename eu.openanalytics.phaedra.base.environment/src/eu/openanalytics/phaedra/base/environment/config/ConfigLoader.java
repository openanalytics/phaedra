package eu.openanalytics.phaedra.base.environment.config;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;

import eu.openanalytics.phaedra.base.environment.Activator;

public class ConfigLoader {

	public static final String CONFIG_PROP = "phaedra.config";
	
	public static Config loadConfig() throws IOException {
		InputStream configInput = null;
		try {
			String configURL = System.getProperty(CONFIG_PROP);
			if (configURL == null) {
				// Read the default config file.
				configInput = FileLocator.openStream(Activator.getDefault().getBundle(), new Path("config.xml"), false);
			} else {
				// Read the config file from the given URL.
				configInput = new URL(configURL).openStream();
			}
			// Parse the config.
			return new Config(configInput);
		} finally {
			if (configInput != null) {
				try {
					configInput.close();
				} catch (IOException e) {}
			}
		}
	}
}
