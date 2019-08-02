package eu.openanalytics.phaedra.ui.curve.prefs;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.StringConverter;
import org.eclipse.swt.graphics.RGB;

import eu.openanalytics.phaedra.ui.curve.Activator;


public class Prefs extends AbstractPreferenceInitializer {

	public final static String CRC_POINT_SIZE = "CRC_POINT_SIZE";
	public final static String CRC_POINT_COLOR_ACCEPTED = "CRC_POINT_COLOR_ACCEPTED";
	public final static String CRC_POINT_COLOR_REJECTED = "CRC_POINT_COLOR_REJECTED";
	
	public final static String CRC_CURVE_COLOR = "CRC_CURVE_COLOR";
	public final static String CRC_CURVE_THICKNESS = "CRC_CURVE_THICKNESS";

	public final static String CRC_SHOW_WEIGHTS = "CRC_SHOW_WEIGHTS";
	
	public static final String CRC_TABLE_FAVORITES_NAMES = "CRC_TABLE_FAVORITES_NAMES";
	
	
	@Override
	public void initializeDefaultPreferences() {
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		
		store.setDefault(CRC_POINT_SIZE, 5);
		store.setDefault(CRC_POINT_COLOR_ACCEPTED, StringConverter.asString(new RGB(64, 64, 255)));
		store.setDefault(CRC_POINT_COLOR_REJECTED, StringConverter.asString(new RGB(255, 64, 64)));
		
		store.setDefault(CRC_CURVE_COLOR, StringConverter.asString(new RGB(0, 128, 255)));
		store.setDefault(CRC_CURVE_THICKNESS, 2);
		
		store.setDefault(CRC_SHOW_WEIGHTS, false);
		
		store.setDefault(CRC_TABLE_FAVORITES_NAMES, "");
	}
	
	
	public static List<String> toNameList(String prefValue) {
		if (prefValue == null || prefValue.isEmpty()) {
			return Collections.emptyList();
		}
		return Arrays.asList(prefValue.split("\\,"));
	}
	
}
