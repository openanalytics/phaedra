package eu.openanalytics.phaedra.ui.wellimage.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import eu.openanalytics.phaedra.ui.wellimage.Activator;

public class Prefs extends AbstractPreferenceInitializer {

	public static final String AUTO_MOVE_CURSOR = "AUTO_MOVE_CURSOR";
	public static final String MAX_THUMBNAIL_SIZE = "MAX_THUMBNAIL_SIZE";

	public static final String WELL_IMAGE_TOOLTIP_SCALE = "WELL_IMAGE_TOOLTIP_SCALE";
	public static final String SUBWELL_IMAGE_TOOLTIP_SCALE = "SUBWELL_IMAGE_TOOLTIP_SCALE";
	
	public static final String IMAGE_TOOLTIP_MAX_X = "IMAGE_TOOLTIP_MAX_X";
	public static final String IMAGE_TOOLTIP_MAX_Y = "IMAGE_TOOLTIP_MAX_Y";
	
	public static final String SHOW_IMAGE_TOOLTIP = "SHOW_IMAGE_TOOLTIP";
	public static final String SHOW_TEXT_TOOLTIP = "SHOW_TEXT_TOOLTIP";
	public static final String SHOW_ADVANCED_TOOLTIP = "SHOW_ADVANCED_TOOLTIP";

	@Override
	public void initializeDefaultPreferences() {
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		store.setDefault(WELL_IMAGE_TOOLTIP_SCALE, 0.5f);
		store.setDefault(IMAGE_TOOLTIP_MAX_X, 1000);
		store.setDefault(IMAGE_TOOLTIP_MAX_Y, 1000);
		store.setDefault(SUBWELL_IMAGE_TOOLTIP_SCALE, 2f);
		store.setDefault(SHOW_IMAGE_TOOLTIP, true);
		store.setDefault(SHOW_TEXT_TOOLTIP, true);
		store.setDefault(SHOW_ADVANCED_TOOLTIP, false);
		store.setDefault(AUTO_MOVE_CURSOR, false);
		store.setDefault(MAX_THUMBNAIL_SIZE, 500);
	}

}
