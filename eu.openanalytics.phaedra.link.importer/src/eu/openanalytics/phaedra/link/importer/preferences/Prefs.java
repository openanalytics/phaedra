package eu.openanalytics.phaedra.link.importer.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import eu.openanalytics.phaedra.link.importer.Activator;


public class Prefs extends AbstractPreferenceInitializer {

	public final static String DETECT_WELL_FEATURES = "AUTOCREATE_WELL_FEATURES";
	public final static String DETECT_SUBWELL_FEATURES = "AUTOCREATE_SUBWELL_FEATURES";
	
	@Override
	public void initializeDefaultPreferences() {
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		store.setDefault(DETECT_WELL_FEATURES, false);
		store.setDefault(DETECT_SUBWELL_FEATURES, false);
	}
}
