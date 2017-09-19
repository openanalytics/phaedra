package eu.openanalytics.phaedra.base.environment.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;

import eu.openanalytics.phaedra.base.environment.Activator;
import eu.openanalytics.phaedra.base.fs.SMBHelper;

public class ConfigLoader {

	private static final String ALLOW_EMBEDDED = "phaedra.allow.embedded";
	private static final String CONFIG_PROP = "phaedra.config";
	
	public static Config loadConfig() throws IOException {
		String preferredConfig = getPreferredConfig();
		if (preferredConfig != null) return parseConfig(openStream(preferredConfig));
		
		String allowEmbedded = System.getProperty(ALLOW_EMBEDDED);
		if (allowEmbedded == null || Boolean.valueOf(allowEmbedded)) {
			return parseConfig(FileLocator.openStream(Activator.getDefault().getBundle(), new Path("config.xml"), false));
		} else {
			throw new IOException("No configuration file found");
		}
	}

	public static String getPreferredConfig() {
		String preferredConfig = Platform.getPreferencesService().getString(Activator.PLUGIN_ID, CONFIG_PROP, null, null);
		if (preferredConfig == null || preferredConfig.isEmpty()) preferredConfig = System.getProperty(CONFIG_PROP);
		return preferredConfig;
	}
	
	public static void setPreferredConfig(String path) {
		Activator.getDefault().getPreferenceStore().setValue(CONFIG_PROP, path);
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
