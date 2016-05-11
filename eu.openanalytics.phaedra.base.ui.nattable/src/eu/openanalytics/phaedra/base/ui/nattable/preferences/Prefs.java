package eu.openanalytics.phaedra.base.ui.nattable.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import eu.openanalytics.phaedra.base.ui.nattable.Activator;

public class Prefs extends AbstractPreferenceInitializer {

	public static final String INC_COLUMN_HEADER_AUTO_RESIZE = "INC_COLUMN_HEADER_AUTO_RESIZE";

	@Override
	public void initializeDefaultPreferences() {
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		store.setDefault(INC_COLUMN_HEADER_AUTO_RESIZE, true);

	}

}
