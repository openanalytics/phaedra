package eu.openanalytics.phaedra.base.ui.charting.preferences;

import java.awt.Color;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.swt.graphics.RGB;

import eu.openanalytics.phaedra.base.ui.charting.Activator;
import eu.openanalytics.phaedra.base.ui.charting.v2.grouping.DefaultStyleProvider;


public class Prefs extends AbstractPreferenceInitializer {

	public final static String SYMBOL_SIZE = "SYMBOL_SIZE";
	public final static String PADDING = "PADDING";
	public final static String SELECTION_OPACITY = "SELECTION_OPACITY";
	public final static String DEFAULT_COLOR = "DEFAULT_COLOR";
	public final static String DEFAULT_SYMBOL_TYPE = "DEFAULT_SYMBOL_TYPE";
	public final static String DEFAULT_BAR_TYPE = "DEFAULT_BAR_TYPE";
	public final static String SKIP_ZERO_DENSITY = "SKIP_ZERO_DENSITY";
	public final static String UPDATE_FEATURE_ON_FOCUS= "UPDATE_FEATURE_ON_FOCUS";

	public final static String EXPORT_WELL_IMAGE_AS_VECTOR = "EXPORT_WELL_IMAGE_AS_VECTOR";
	public final static String EXPORT_SUBWELL_IMAGE_AS_VECTOR = "EXPORT_SUBWELL_IMAGE_AS_VECTOR";

	@Override
	public void initializeDefaultPreferences() {
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();

		store.setDefault(SYMBOL_SIZE,3);
		store.setDefault(PADDING, 15);
		store.setDefault(SELECTION_OPACITY, 50);
		PreferenceConverter.setDefault(store, DEFAULT_COLOR, new RGB(0,0,255));
		store.setDefault(DEFAULT_SYMBOL_TYPE, DefaultStyleProvider.FILLED_CIRCLE);
		store.setDefault(DEFAULT_BAR_TYPE, DefaultStyleProvider.BARSTYLE_FILLED);
		store.setDefault(SKIP_ZERO_DENSITY, true);
		store.setDefault(UPDATE_FEATURE_ON_FOCUS, true);

		store.setDefault(EXPORT_WELL_IMAGE_AS_VECTOR, true);
		store.setDefault(EXPORT_SUBWELL_IMAGE_AS_VECTOR, false);

	}

	public static Color getDefaultColor() {
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		RGB rgb = PreferenceConverter.getColor(store, DEFAULT_COLOR);

		return new Color(rgb.red, rgb.green, rgb.blue);
	}

}
