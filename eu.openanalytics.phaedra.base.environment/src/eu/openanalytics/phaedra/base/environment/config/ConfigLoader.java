package eu.openanalytics.phaedra.base.environment.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;

import eu.openanalytics.phaedra.base.environment.Activator;
import eu.openanalytics.phaedra.base.fs.SMBHelper;

public class ConfigLoader {

	private static final String CONFIG_PROP = "phaedra.config";
	
	public static Config loadConfig() throws IOException {
		InputStream config = null;
		
		String configPath = System.getProperty(CONFIG_PROP);
		if (configPath != null) config = openStream(configPath);

		if (config == null) {
			config = FileLocator.openStream(Activator.getDefault().getBundle(), new Path("config.xml"), false);
		}
		
		return parseConfig(config);
	}

	private static InputStream openStream(String path) throws IOException {
		if (SMBHelper.isSMBPath(path)) {
			return SMBHelper.open(path);
		} else if (new File(path).isFile()) {
			return new FileInputStream(path);
		} else {
			return new URL(path).openStream();
		}
	}
	
	private static Config parseConfig(InputStream input) throws IOException {
		try {
			return new Config(input);
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {}
			}
		}
	}
}
