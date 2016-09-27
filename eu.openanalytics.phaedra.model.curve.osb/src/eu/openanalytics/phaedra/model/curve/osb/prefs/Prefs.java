package eu.openanalytics.phaedra.model.curve.osb.prefs;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.StringConverter;
import org.eclipse.swt.graphics.RGB;

import eu.openanalytics.phaedra.model.curve.osb.Activator;

public class Prefs extends AbstractPreferenceInitializer {

	public final static String CRC_SHOW_PIC50_MARKER = "CRC_SHOW_PIC50_MARKER";
	public final static String CRC_SHOW_OTHER_IC_MARKERS = "CRC_SHOW_OTHER_IC_MARKERS";
	
	public final static String CRC_SHOW_CONF_AREA = "CRC_SHOW_CONF_AREA";
	public final static String CRC_CONF_AREA_ALPHA = "CRC_CONF_AREA_ALPHA";
	public final static String CRC_CONF_AREA_COLOR = "CRC_CONF_AREA_COLOR";
	
	public final static String CRC_BOUND_THICKNESS = "CRC_BOUND_THICKNESS";
	public final static String CRC_BOUND_COLOR_UPPER = "CRC_BOUND_COLOR_UPPER";
	public final static String CRC_BOUND_COLOR_LOWER = "CRC_BOUND_COLOR_LOWER";
	
	@Override
	public void initializeDefaultPreferences() {
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		
		store.setDefault(CRC_SHOW_PIC50_MARKER, true);
		store.setDefault(CRC_SHOW_OTHER_IC_MARKERS, false);
		
		store.setDefault(CRC_SHOW_CONF_AREA, true);
		store.setDefault(CRC_CONF_AREA_ALPHA, 25);
		store.setDefault(CRC_CONF_AREA_COLOR, StringConverter.asString(new RGB(255, 255, 128)));
		
		store.setDefault(CRC_BOUND_THICKNESS, 3);
		store.setDefault(CRC_BOUND_COLOR_UPPER, StringConverter.asString(new RGB(0, 255, 0)));
		store.setDefault(CRC_BOUND_COLOR_LOWER, StringConverter.asString(new RGB(255, 0, 0)));
	}
}
