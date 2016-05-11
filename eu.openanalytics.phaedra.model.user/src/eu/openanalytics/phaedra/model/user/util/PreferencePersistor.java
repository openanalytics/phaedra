package eu.openanalytics.phaedra.model.user.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.openanalytics.phaedra.base.pref.store.IPreferencePersistor;
import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.model.user.UserService;
import eu.openanalytics.phaedra.model.user.vo.Preference;

/**
 * Saves and loads user preferences from the database and
 * passes them to the preference store plugin.
 */
public class PreferencePersistor implements IPreferencePersistor {

	private final static String USER_PREF_TYPE = "UserPreference";
	
	@Override
	public void savePreferences(Map<String, String> preferences) {
		String userName = SecurityService.getInstance().getCurrentUserName();
		List<Preference> prefs = UserService.getInstance().getPreferences(USER_PREF_TYPE);
		for (String key: preferences.keySet()) {
			Preference newPref = new Preference();
			newPref.setItem(key);
			newPref.setUser(userName);
			newPref.setType(USER_PREF_TYPE);
			
			newPref = findOrAdd(newPref, prefs);
			newPref.setValue(preferences.get(key));
		}
		if (!prefs.isEmpty()) {
			UserService.getInstance().savePreferences(prefs);
		}
	}

	@Override
	public Map<String, String> loadPreferences() {
		// Load all user preferences for the current user.
		List<Preference> prefs = UserService.getInstance().getPreferences(USER_PREF_TYPE);
		Map<String,String> prefMap = new HashMap<String, String>();
		for (Preference pref: prefs) {
			prefMap.put(pref.getItem(), pref.getValue());
		}
		return prefMap;
	}

	private Preference findOrAdd(Preference pref, List<Preference> prefs) {
		for (Preference p: prefs) {
			if (p.equals(pref)) return p;
		}
		prefs.add(pref);
		return pref;
	}
}
