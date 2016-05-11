package eu.openanalytics.phaedra.base.pref.store;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;

public class GlobalPrefenceAccessor {

	public static void loadPreferences() {
		IPreferencePersistor[] persistors = getPersistors();
		if (persistors.length == 0) return;
		
		IPreferencePersistor persistor = persistors[0];
		Map<String, String> prefs = persistor.loadPreferences();
		applyAll(prefs);
	}
	
	public static void savePreferences() {
		Map<String, String> prefs = collectAll();
		
		IPreferencePersistor[] persistors = getPersistors();
		for (IPreferencePersistor persistor: persistors) {
			persistor.savePreferences(prefs);
		}
	}
	
	/*
	 * Non-public
	 */
	
	private static IPreferencePersistor[] getPersistors() {
		List<IPreferencePersistor> persistors = new ArrayList<IPreferencePersistor>();
		
		IConfigurationElement[] config = Platform.getExtensionRegistry()
			.getConfigurationElementsFor(IPreferencePersistor.EXT_POINT_ID);
		for (IConfigurationElement el : config) {
			try {
				Object o = el.createExecutableExtension(IPreferencePersistor.ATTR_APP_CLASS);
				if (o instanceof IPreferencePersistor) {
					IPreferencePersistor persistor = (IPreferencePersistor)o;
					persistors.add(persistor);
				}
			} catch (CoreException e) {
				// Invalid extension.
			}
		}
		
		return persistors.toArray(new IPreferencePersistor[persistors.size()]);
	}
	
	private static Map<String,String> collectAll() {
		Map<String,String> allPrefs = new HashMap<String, String>();
		
		IConfigurationElement[] config = Platform.getExtensionRegistry()
			.getConfigurationElementsFor(IPreferenceProvider.EXT_POINT_ID);
		for (IConfigurationElement el : config) {
			try {
				Object o = el.createExecutableExtension(IPreferenceProvider.ATTR_APP_CLASS);
				if (o instanceof IPreferenceProvider) {
					String pluginId = el.getContributor().getName();
					IPreferenceProvider provider = (IPreferenceProvider)o;
					Map<String,String> prefs = provider.collectPreferences();
					for (String key: prefs.keySet()) {
						String value = prefs.get(key);
						if (!key.startsWith(pluginId)) {
							key = pluginId + "::" + key;
						}
						allPrefs.put(key, value);
					}
				}
			} catch (CoreException e) {
				// Invalid extension.
			}
		}
		
		return allPrefs;
	}
	
	private static void applyAll(Map<String,String> preferences) {
		Map<String,Map<String,String>> prefsPerPlugin = new HashMap<String, Map<String,String>>();
		for (String key: preferences.keySet()) {
			if (key.contains("::")) {
				String pluginId = key.substring(0, key.indexOf("::"));
				String prefKey = key.substring(key.indexOf("::")+2);
				Map<String,String> prefs = prefsPerPlugin.get(pluginId);
				if (prefs == null) {
					prefs = new HashMap<String,String>();
					prefsPerPlugin.put(pluginId, prefs);
				}
				prefs.put(prefKey, preferences.get(key));
			}
		}
		
		IConfigurationElement[] config = Platform.getExtensionRegistry()
			.getConfigurationElementsFor(IPreferenceProvider.EXT_POINT_ID);
		for (IConfigurationElement el : config) {
			try {
			Object o = el.createExecutableExtension(IPreferenceProvider.ATTR_APP_CLASS);
			if (o instanceof IPreferenceProvider) {
				String pluginId = el.getContributor().getName();
				IPreferenceProvider provider = (IPreferenceProvider)o;
				Map<String,String> prefs = prefsPerPlugin.get(pluginId);
				if (prefs != null && !prefs.isEmpty()) {
					provider.applyPreferences(prefs);
				}
			}
			} catch (CoreException e) {
				// Invalid extension.
			}
		}
	}
}
