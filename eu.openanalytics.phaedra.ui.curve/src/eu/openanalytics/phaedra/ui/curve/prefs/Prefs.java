package eu.openanalytics.phaedra.ui.curve.prefs;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.StringConverter;
import org.eclipse.swt.graphics.RGB;

import eu.openanalytics.phaedra.ui.curve.Activator;


public class Prefs extends AbstractPreferenceInitializer {

	public final static String CRC_SHOW_PIC50_MARKER = "CRC_SHOW_PIC50_MARKER";
	public final static String CRC_SHOW_OTHER_IC_MARKERS = "CRC_SHOW_OTHER_IC_MARKERS";
	
	public final static String CRC_SHOW_CONF_AREA = "CRC_SHOW_CONF_AREA";
	public final static String CRC_CONF_AREA_ALPHA = "CRC_CONF_AREA_ALPHA";
	public final static String CRC_CONF_AREA_COLOR = "CRC_CONF_AREA_COLOR";
	
	public final static String CRC_POINT_SIZE = "CRC_POINT_SIZE";
	public final static String CRC_POINT_COLOR_ACCEPTED = "CRC_POINT_COLOR_ACCEPTED";
	public final static String CRC_POINT_COLOR_REJECTED = "CRC_POINT_COLOR_REJECTED";
	
	public final static String CRC_BOUND_THICKNESS = "CRC_BOUND_THICKNESS";
	public final static String CRC_BOUND_COLOR_UPPER = "CRC_BOUND_COLOR_UPPER";
	public final static String CRC_BOUND_COLOR_LOWER = "CRC_BOUND_COLOR_LOWER";
	
	public final static String CRC_CURVE_COLOR = "CRC_CURVE_COLOR";
	public final static String CRC_CURVE_THICKNESS = "CRC_CURVE_THICKNESS";

	public final static String CRC_SHOW_WEIGHTS = "CRC_SHOW_WEIGHTS";
	
	@Override
	public void initializeDefaultPreferences() {
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		
		store.setDefault(CRC_SHOW_PIC50_MARKER, true);
		store.setDefault(CRC_SHOW_OTHER_IC_MARKERS, false);
		store.setDefault(CRC_SHOW_WEIGHTS, false);
		
		store.setDefault(CRC_SHOW_CONF_AREA, true);
		store.setDefault(CRC_CONF_AREA_ALPHA, 25);
		store.setDefault(CRC_CONF_AREA_COLOR, StringConverter.asString(new RGB(255, 255, 128)));
		
		store.setDefault(CRC_POINT_SIZE, 5);
		store.setDefault(CRC_POINT_COLOR_ACCEPTED, StringConverter.asString(new RGB(64, 64, 255)));
		store.setDefault(CRC_POINT_COLOR_REJECTED, StringConverter.asString(new RGB(255, 64, 64)));
		
		store.setDefault(CRC_BOUND_THICKNESS, 2);
		store.setDefault(CRC_BOUND_COLOR_UPPER, StringConverter.asString(new RGB(0, 255, 0)));
		store.setDefault(CRC_BOUND_COLOR_LOWER, StringConverter.asString(new RGB(255, 0, 0)));
		
		store.setDefault(CRC_CURVE_COLOR, StringConverter.asString(new RGB(0, 128, 255)));
		store.setDefault(CRC_CURVE_THICKNESS, 2);
	}
}
