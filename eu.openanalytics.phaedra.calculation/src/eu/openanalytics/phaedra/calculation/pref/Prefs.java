package eu.openanalytics.phaedra.calculation.pref;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import eu.openanalytics.phaedra.calculation.Activator;

public class Prefs extends AbstractPreferenceInitializer {

	public final static String WDA_MAX_FEATURE_QUEUE = "WDA_MAX_FEATURE_QUEUE";
	public final static String WDA_MAX_WELL_QUEUE = "WDA_MAX_WELL_QUEUE";
	public final static String WDA_MAX_QUERY_ITEMS = "WDA_MAX_QUERY_ITEMS";
	public final static String WDA_REFRESH_DELAY = "WDA_REFRESH_DELAY";
	public final static String WDA_PRELOAD = "WDA_PRELOAD";
	
	public final static String AUTO_DEFINE_ANNOTATIONS = "AUTO_DEFINE_ANNOTATIONS";

	@Override
	public void initializeDefaultPreferences() {
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();

		store.setDefault(WDA_REFRESH_DELAY, 400);
		store.setDefault(WDA_MAX_QUERY_ITEMS, 5000);
		store.setDefault(WDA_MAX_WELL_QUEUE, 900);
		store.setDefault(WDA_MAX_FEATURE_QUEUE, 200);
		store.setDefault(WDA_PRELOAD, true);
		
		store.setDefault(AUTO_DEFINE_ANNOTATIONS, false);
	}
}
