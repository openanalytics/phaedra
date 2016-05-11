package eu.openanalytics.phaedra.datacapture.columbus.prefs;

import org.eclipse.ui.preferences.ScopedPreferenceStore;

import eu.openanalytics.phaedra.base.pref.store.AbstractPreferenceProvider;
import eu.openanalytics.phaedra.datacapture.columbus.Activator;

public class PreferenceProvider extends AbstractPreferenceProvider {

	@Override
	protected ScopedPreferenceStore getPreferenceStore() {
		return (ScopedPreferenceStore) Activator.getDefault().getPreferenceStore();
	}

}
