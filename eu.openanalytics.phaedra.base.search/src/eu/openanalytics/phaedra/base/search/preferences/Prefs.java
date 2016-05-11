package eu.openanalytics.phaedra.base.search.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import eu.openanalytics.phaedra.base.search.Activator;

public class Prefs extends AbstractPreferenceInitializer {

	public final static String DEFAULT_MAX_RESULTS = "DEFAULT_MAX_RESULTS";

	@Override
	public void initializeDefaultPreferences() {
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		store.setDefault(DEFAULT_MAX_RESULTS, 100);		
	}
}
