package eu.openanalytics.phaedra.base.pref.store;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.osgi.service.prefs.BackingStoreException;

public abstract class AbstractPreferenceProvider implements IPreferenceProvider {

	/**
	 * Obtain the preference store that contains the local preferences.
	 * Usually obtained from the plugin Activator.
	 * 
	 * @return The ScopedPreferenceStore used for collecting/applying preferences.
	 */
	protected abstract ScopedPreferenceStore getPreferenceStore();
	
	@Override
	public Map<String, String> collectPreferences() {
		ScopedPreferenceStore prefStore = getPreferenceStore();
		Map<String,String> prefs = new HashMap<String, String>();
		try {
			for (IEclipsePreferences preferences: prefStore.getPreferenceNodes(true)) {
				for (String key : preferences.keys()) {
					prefs.put(key, prefStore.getString(key));
				}
			}
		} catch (BackingStoreException e) {
			throw new RuntimeException("Failed to list preferences", e);
		}
		return prefs;
	}

	@Override
	public void applyPreferences(Map<String, String> preferences) {
		ScopedPreferenceStore prefStore = getPreferenceStore();
		for (String key: preferences.keySet()) {
			String value = preferences.get(key);
			if (key != null && value != null) {
				prefStore.putValue(key, value);
			}
		}
	}

}
