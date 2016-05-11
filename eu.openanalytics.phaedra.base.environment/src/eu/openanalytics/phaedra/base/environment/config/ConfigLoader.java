package eu.openanalytics.phaedra.base.environment.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;

import eu.openanalytics.phaedra.base.environment.Activator;

public class ConfigLoader {

	public final static String CONFIG_PATH = "config.xml";
	
	public static Config loadConfig() {
		InputStream configInput = null;
		try {
			File customConfigFile = new File(CONFIG_PATH);
			if (customConfigFile.exists()) {
				// A custom config file is present in the working directory, use it.
				configInput = new FileInputStream(customConfigFile);
			} else {
				// No custom config, use the defaults.
				configInput = FileLocator.openStream(Activator.getDefault().getBundle(), new Path(CONFIG_PATH), false);
			}
			// Parse the config.
			return new Config(configInput);
		} catch (Throwable t) {
			throw new RuntimeException("Failed to parse config file", t);
		} finally {
			if (configInput != null) {
				try {
					configInput.close();
				} catch (IOException e) {}
			}
		}
	}
}
