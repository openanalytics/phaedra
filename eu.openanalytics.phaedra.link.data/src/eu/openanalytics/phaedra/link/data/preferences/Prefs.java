package eu.openanalytics.phaedra.link.data.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import eu.openanalytics.phaedra.link.data.Activator;


public class Prefs extends AbstractPreferenceInitializer {

	public final static String DEFAULT_CAPTURED_DATA_FOLDER = "DEFAULT_CAPTURED_DATA_FOLDER";
	
	@Override
	public void initializeDefaultPreferences() {
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		store.setDefault(DEFAULT_CAPTURED_DATA_FOLDER, "");
	}
}
