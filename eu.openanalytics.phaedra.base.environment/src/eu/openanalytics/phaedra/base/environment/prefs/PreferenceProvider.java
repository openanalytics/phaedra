package eu.openanalytics.phaedra.base.environment.prefs;

import org.eclipse.ui.preferences.ScopedPreferenceStore;

import eu.openanalytics.phaedra.base.environment.Activator;
import eu.openanalytics.phaedra.base.pref.store.AbstractPreferenceProvider;



/**
 * Contribute the preferences of this plugin to the global pref store,
 * allowing them to be persisted to a common store.
 */
public class PreferenceProvider extends AbstractPreferenceProvider {

	@Override
	protected ScopedPreferenceStore getPreferenceStore() {
		return (ScopedPreferenceStore)Activator.getDefault().getPreferenceStore();
	}

}