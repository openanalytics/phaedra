package eu.openanalytics.phaedra.base.pref.store;

import java.util.Map;

public interface IPreferencePersistor {

	public final static String EXT_POINT_ID = Activator.PLUGIN_ID + ".preferencePersistor";
	public final static String ATTR_APP_CLASS = "class";
	
	public void savePreferences(Map<String,String> preferences);
	
	public Map<String,String> loadPreferences();
}
