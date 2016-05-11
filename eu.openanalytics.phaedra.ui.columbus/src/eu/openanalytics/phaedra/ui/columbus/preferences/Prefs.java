package eu.openanalytics.phaedra.ui.columbus.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import eu.openanalytics.phaedra.ui.columbus.Activator;


public class Prefs extends AbstractPreferenceInitializer {

	public final static String DEFAULT_SOURCE_PATH = "DEFAULT_SOURCE_PATH";
	public final static String DEFAULT_IMAGE_PATH = "DEFAULT_IMAGE_PATH";
	public final static String DEFAULT_SUBWELL_DATA_PATH = "DEFAULT_SUBWELL_DATA_PATH";
	
	@Override
	public void initializeDefaultPreferences() {
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		store.setDefault(DEFAULT_SOURCE_PATH, "");
		store.setDefault(DEFAULT_IMAGE_PATH, "");
		store.setDefault(DEFAULT_SUBWELL_DATA_PATH, "");
	}
}
