package eu.openanalytics.phaedra.base.environment.prefs;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import eu.openanalytics.phaedra.base.environment.Activator;

public class Prefs extends AbstractPreferenceInitializer {

	public final static String USE_ALL_PHYS_CORES = "USE_ALL_PHYS_CORES";
	public final static String USE_ALL_LOG_CORES = "USE_ALL_LOG_CORES";
	public final static String THREAD_POOL_SIZE = "THREAD_POOL_SIZE";
	
	public final static String R_POOL_SIZE = "R_POOL_SIZE";
	public final static String USE_PARALLEL_SUBWELL_LOADING = "USE_PARALLEL_SUBWELL_LOADING";
	
	@Override
	public void initializeDefaultPreferences() {
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		store.setDefault(USE_ALL_PHYS_CORES, true);
		store.setDefault(USE_ALL_LOG_CORES, false);
		store.setDefault(THREAD_POOL_SIZE, 4);
		
		store.setDefault(R_POOL_SIZE, 2);
		store.setDefault(USE_PARALLEL_SUBWELL_LOADING, true);
	}
}
