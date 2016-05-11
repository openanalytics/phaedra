package eu.openanalytics.phaedra.base.cache.prefs;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import eu.openanalytics.phaedra.base.cache.Activator;

public class Prefs extends AbstractPreferenceInitializer {

	public final static String CACHE_HEAP_PCT = "CACHE_HEAP_PCT";
	
	public final static String DEFAULT_CACHE_TTI = "DEFAULT_CACHE_TTI";
	public final static String DEFAULT_CACHE_TTL = "DEFAULT_CACHE_TTL";
	
	public final static String CACHE_DISK_SIZE = "CACHE_DISK_SIZE";
	public final static String CACHE_DISK_BUFFER_SIZE = "CACHE_DISK_BUFFER_SIZE";
	
	public final static String DEFAULT_CACHE_USE_DISK = "DEFAULT_CACHE_USE_DISK";

	@Override
	public void initializeDefaultPreferences() {
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		store.setDefault(CACHE_HEAP_PCT, 75);
		store.setDefault(CACHE_DISK_SIZE, 0);
		store.setDefault(CACHE_DISK_BUFFER_SIZE, 200);
		store.setDefault(DEFAULT_CACHE_USE_DISK, false);
		store.setDefault(DEFAULT_CACHE_TTI, 3600);
		store.setDefault(DEFAULT_CACHE_TTL, 7200);
	}
	
	public static int getInt(String name) {
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		return store.getInt(name);
	}
	
	public static boolean getBoolean(String name) {
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		return store.getBoolean(name);
	}
}
