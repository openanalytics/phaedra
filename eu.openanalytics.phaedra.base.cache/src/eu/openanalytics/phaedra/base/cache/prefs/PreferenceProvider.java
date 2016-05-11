package eu.openanalytics.phaedra.base.cache.prefs;

import org.eclipse.ui.preferences.ScopedPreferenceStore;

import eu.openanalytics.phaedra.base.cache.Activator;
import eu.openanalytics.phaedra.base.pref.store.AbstractPreferenceProvider;

public class PreferenceProvider extends AbstractPreferenceProvider {

	@Override
	protected ScopedPreferenceStore getPreferenceStore() {
		return (ScopedPreferenceStore)Activator.getDefault().getPreferenceStore();
	}

}