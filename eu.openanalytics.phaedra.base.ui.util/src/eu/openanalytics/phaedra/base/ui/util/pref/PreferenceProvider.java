package eu.openanalytics.phaedra.base.ui.util.pref;

import org.eclipse.ui.preferences.ScopedPreferenceStore;

import eu.openanalytics.phaedra.base.pref.store.AbstractPreferenceProvider;
import eu.openanalytics.phaedra.base.ui.util.Activator;

public class PreferenceProvider extends AbstractPreferenceProvider {

	@Override
	protected ScopedPreferenceStore getPreferenceStore() {
		return (ScopedPreferenceStore) Activator.getDefault().getPreferenceStore();
	}

}
