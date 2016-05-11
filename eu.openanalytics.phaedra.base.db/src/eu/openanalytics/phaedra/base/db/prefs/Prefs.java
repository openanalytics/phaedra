package eu.openanalytics.phaedra.base.db.prefs;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import eu.openanalytics.phaedra.base.db.Activator;

public class Prefs extends AbstractPreferenceInitializer {

	public final static String DB_POOL_SIZE = "DB_POOL_SIZE";
	public final static String DB_TIME_OUT = "DB_TIME_OUT";

	@Override
	public void initializeDefaultPreferences() {
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();

		store.setDefault(DB_POOL_SIZE, 4);
		store.setDefault(DB_TIME_OUT, 60000);
	}
}
