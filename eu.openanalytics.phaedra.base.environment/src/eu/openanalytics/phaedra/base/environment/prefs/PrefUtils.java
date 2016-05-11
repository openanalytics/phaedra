package eu.openanalytics.phaedra.base.environment.prefs;

import org.eclipse.jface.preference.IPreferenceStore;

import eu.openanalytics.phaedra.base.environment.Activator;
import eu.openanalytics.phaedra.base.util.process.ProcessUtils;

public class PrefUtils {

	public static int getNumberOfThreads() {
		IPreferenceStore prefs = Activator.getDefault().getPreferenceStore();
		
		if (prefs.getBoolean(Prefs.USE_ALL_PHYS_CORES)) {
			return ProcessUtils.getPhysicalCores();
		} else if (prefs.getBoolean(Prefs.USE_ALL_LOG_CORES)) {
			return ProcessUtils.getLogicalCores();
		} else {
			return prefs.getInt(Prefs.THREAD_POOL_SIZE);
		}
	}

	public static IPreferenceStore getPrefStore() {
		return Activator.getDefault().getPreferenceStore();
	}
}