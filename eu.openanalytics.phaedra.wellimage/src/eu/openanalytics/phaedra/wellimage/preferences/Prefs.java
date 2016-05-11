package eu.openanalytics.phaedra.wellimage.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import eu.openanalytics.phaedra.wellimage.Activator;

public class Prefs extends AbstractPreferenceInitializer {

	/**
	 * SubWell Image Is Absolute Padding.
	 */
	public static final String SW_IMG_IS_ABS_PADDING = "SW_IMG_IS_ABS_PADDING";
	/**
	 * SubWell Image Absolute Padding.
	 */
	public static final String SW_IMG_ABS_PADDING = "SW_IMG_ABS_PADDING";
	/**
	 * SubWell Image Relative Padding.
	 */
	public static final String SW_IMG_REL_PADDING = "SW_IMG_REL_PADDING";

	@Override
	public void initializeDefaultPreferences() {
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		store.setDefault(SW_IMG_IS_ABS_PADDING, true);
		store.setDefault(SW_IMG_ABS_PADDING, 10);
		store.setDefault(SW_IMG_REL_PADDING, 10);
	}

}
