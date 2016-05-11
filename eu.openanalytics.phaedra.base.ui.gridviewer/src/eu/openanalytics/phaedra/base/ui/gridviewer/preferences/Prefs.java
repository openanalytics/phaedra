package eu.openanalytics.phaedra.base.ui.gridviewer.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import eu.openanalytics.phaedra.base.ui.gridviewer.Activator;

public class Prefs extends AbstractPreferenceInitializer {

	public final static String GRID_TOOLTIPS = "GRID_VIEWER_TOOLTIPS";
	
	@Override
	public void initializeDefaultPreferences() {
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		store.setDefault(GRID_TOOLTIPS, false);
	}
	
}