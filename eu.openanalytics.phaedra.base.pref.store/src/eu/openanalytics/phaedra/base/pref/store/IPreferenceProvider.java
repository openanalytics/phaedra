package eu.openanalytics.phaedra.base.pref.store;

import java.util.Map;

public interface IPreferenceProvider {

	public final static String EXT_POINT_ID = Activator.PLUGIN_ID + ".preferenceProvider";
	public final static String ATTR_APP_CLASS = "class";
	
	/**
	 * Collect all non-default preferences that this provider
	 * knows about. Usually, this is the contents of the whole
	 * instance preference store of a plugin.
	 * 
	 * @return A key-value map of collected preferences.
	 */
	public Map<String,String> collectPreferences();
	
	/**
	 * Replace the preference values of this provider with
	 * the values given by the key-value map.
	 * 
	 * @param preferences The map of preference values to set.
	 */
	public void applyPreferences(Map<String,String> preferences);
}
