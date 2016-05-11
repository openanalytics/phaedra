package eu.openanalytics.phaedra.calculation.pref;

import org.eclipse.ui.preferences.ScopedPreferenceStore;

import eu.openanalytics.phaedra.base.pref.store.AbstractPreferenceProvider;
import eu.openanalytics.phaedra.calculation.Activator;

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