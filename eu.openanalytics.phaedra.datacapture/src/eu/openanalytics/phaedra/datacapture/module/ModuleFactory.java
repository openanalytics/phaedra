package eu.openanalytics.phaedra.datacapture.module;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;

import eu.openanalytics.phaedra.base.environment.IEnvironment;
import eu.openanalytics.phaedra.base.environment.Screening;
import eu.openanalytics.phaedra.datacapture.DataCaptureException;
import eu.openanalytics.phaedra.datacapture.DataCaptureTask;
import eu.openanalytics.phaedra.datacapture.config.CaptureConfig;
import eu.openanalytics.phaedra.datacapture.config.ModuleConfig;
import eu.openanalytics.phaedra.datacapture.config.parser.ModuleConfigParser;

public class ModuleFactory {


	private final static String CONFIG_REPO_PATH = "/data.capture.configurations";
	
	public static IModule[] createAndConfigureModules(DataCaptureTask task) throws IOException, DataCaptureException {
		CaptureConfig captureConfig = loadCaptureConfig(task.getConfigId());
		if (captureConfig == null) throw new DataCaptureException("Capture config not found: " + task.getConfigId());
		ModuleConfig[] configs = captureConfig.getModuleConfigs();
		
		IModule[] modules = new IModule[configs.length];
		for (int i=0; i<configs.length; i++) {
			modules[i] = createModule(configs[i]);
			modules[i].configure(configs[i]);
		}
		return modules;
	}
	
	public static CaptureConfig loadCaptureConfig(String id) throws IOException {
		InputStream input = loadConfig(id);
		if (input == null) return null;
		CaptureConfig captureConfig = new ModuleConfigParser().parse(input);
		captureConfig.setId(id);
		return captureConfig;
	}
	
	public static void saveCaptureConfig(String id, byte[] contents) throws IOException {
		String savePath = CONFIG_REPO_PATH + "/" + id + ".xml";
		Screening.getEnvironment().getFileServer().putContents(savePath, contents);
	}

	/*
	 * retrieve the unique data capture configuration IDs from the xml file on the Phaedra server
	 */
	public static String[] getAvailableConfigs() throws IOException{
		List<String> items = Screening.getEnvironment().getFileServer().dir(CONFIG_REPO_PATH);
		List<String> configs = new ArrayList<String>();
		for (String item: items) {
			if (item.toLowerCase().endsWith(".xml")) {
				configs.add(item.substring(0, item.length()-4));
			}
		}
		return configs.toArray(new String[configs.size()]);
	}
	
	public static IModule createModule(ModuleConfig cfg) throws DataCaptureException {
		String type = cfg.getType();
		IModule module = loadContributedModule(type);
		return module;
	}
	
	public static ModuleConfig getModuleConfig(CaptureConfig captureConfig, String moduleId) {
		ModuleConfig matchingConfig = null;
		for (ModuleConfig moduleConfig: captureConfig.getModuleConfigs()) {
			if (moduleConfig.getId().equals(moduleId)) {
				matchingConfig = moduleConfig;
				break;
			}
		}
		return matchingConfig;
	}
	
	/*
	 * **********
	 * Non-public
	 * **********
	 */
	
	private static InputStream loadConfig(String id) throws IOException {
		IEnvironment env = Screening.getEnvironment();
		List<String> items = env.getFileServer().dir(CONFIG_REPO_PATH);
		for (String item: items) {
			String path = CONFIG_REPO_PATH + "/" + item;
			if (env.getFileServer().isDirectory(path)) continue;
			String itemId = item;
			if (itemId.toLowerCase().endsWith(".xml")) {
				itemId = itemId.substring(0,itemId.length()-4);
			}
			if (itemId.equals(id)) {
				return env.getFileServer().getContents(path);
			}
		}
		return null;
	}
	
	private static IModule loadContributedModule(String type) {
		IConfigurationElement[] config = Platform.getExtensionRegistry()
				.getConfigurationElementsFor(IModule.EXT_PT_ID);
		for (IConfigurationElement el : config) {
			String elementType = el.getAttribute(IModule.ATTR_TYPE);
			if (elementType.equals(type)) {
				try {
					Object o = el.createExecutableExtension(IModule.ATTR_CLASS);
					if (o instanceof IModule) {
						IModule module = (IModule)o;
						return module;
					}
				} catch (CoreException e) {
					throw new IllegalArgumentException("Invalid module: " + type);
				}
			}
		}
		return null;
	}
}
