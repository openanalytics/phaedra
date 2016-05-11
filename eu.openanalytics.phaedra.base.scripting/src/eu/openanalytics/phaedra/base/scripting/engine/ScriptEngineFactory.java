package eu.openanalytics.phaedra.base.scripting.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;

import eu.openanalytics.phaedra.base.scripting.Activator;
import eu.openanalytics.phaedra.base.scripting.api.IScriptAPIProvider;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;

/**
 * Obtain available script engines via the IScriptEngine extension point.
 */
public class ScriptEngineFactory {
	
	public static String[] getIds() {
		List<String> ids = new ArrayList<>();
		IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(IScriptEngine.EXT_PT_ID);
		
		for (IConfigurationElement el : config) {
			String engineId = el.getAttribute(IScriptEngine.ATTR_ID);
			ids.add(engineId);
		}
		
		return ids.toArray(new String[ids.size()]);
	}
	
	public static IScriptEngine createEngine(String id) {
		
		IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(IScriptEngine.EXT_PT_ID);
		
		for (IConfigurationElement el : config) {
			try {
				String engineId = el.getAttribute(IScriptEngine.ATTR_ID);
				if (id.equals(engineId)) {
					Object o = el.createExecutableExtension(IScriptEngine.ATTR_CLASS);
					if (o instanceof IScriptEngine) {
						IScriptEngine engine = (IScriptEngine)o;
						return engine;
					}
				}
			} catch (CoreException e) {
				// Invalid extension.
				EclipseLog.error("Failed to instantiate script engine " + id, e, Activator.getDefault());
			}
		}
		
		return null;
	}
	
	public static void loadEngineServices(IScriptEngine engine) {
		IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(IScriptAPIProvider.EXT_PT_ID);
		for (IConfigurationElement el : config) {
			try {
				IScriptAPIProvider provider = (IScriptAPIProvider) el.createExecutableExtension(IScriptAPIProvider.ATTR_CLASS);
				Map<String, Object> services = provider.getServices();
				for (String service: services.keySet()) {
					engine.registerAPI(service, services.get(service), provider.getHelp(service));
				}
			} catch (CoreException e) {
				// Ignore invalid extensions.
			}
		}
	}
}
