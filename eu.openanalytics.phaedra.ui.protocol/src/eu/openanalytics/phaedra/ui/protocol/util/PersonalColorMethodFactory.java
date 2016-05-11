package eu.openanalytics.phaedra.ui.protocol.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.openanalytics.phaedra.base.cache.CacheService;
import eu.openanalytics.phaedra.base.cache.ICache;
import eu.openanalytics.phaedra.base.event.ModelEvent;
import eu.openanalytics.phaedra.base.event.ModelEventService;
import eu.openanalytics.phaedra.base.event.ModelEventType;
import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.base.ui.colormethod.ColorMethodRegistry;
import eu.openanalytics.phaedra.base.ui.colormethod.IColorMethod;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.model.user.UserService;
import eu.openanalytics.phaedra.model.user.vo.Preference;

public class PersonalColorMethodFactory {

	public final static String PREF_TYPE_CM = "ColorMethod";

	public static IColorMethod getColorMethod(Feature feature) {
		if (feature == null) return null;

		// First, look in the cache.
		String cacheKey = getCacheKey(feature);
		boolean isCached = getCache().contains(cacheKey);
		if (isCached) {
			// Always return a fresh instance.
			// Since color methods are often used concurrently, shared instances are not appropriate here.
			IColorMethod cachedMethod = (IColorMethod) getCache().get(cacheKey);
			if (cachedMethod == null) return null;
			IColorMethod newMethod = ColorMethodRegistry.getInstance().createMethod(cachedMethod.getId());
			if (newMethod == null) return null;
			Map<String,String> settings = new HashMap<>();
			cachedMethod.getConfiguration(settings);
			newMethod.configure(settings);
			return newMethod;
		}

		// Then, look in the user's preferences.
		IColorMethod method = null;
		String id = UserService.getInstance().getPreferenceValue(PREF_TYPE_CM, getPrefKey(feature, ColorMethodFactory.SETTING_METHOD_ID));
		if (id != null) method = ColorMethodRegistry.getInstance().createMethod(id);
		getCache().put(cacheKey, method);

		if (method != null) method.configure(getSettings(feature));
		return method;
	}

	public static void saveColorMethod(Feature f, IColorMethod cm) {

		Map<String,String> settingsMap = new HashMap<String, String>();
		cm.getConfiguration(settingsMap);
		settingsMap.put(ColorMethodFactory.SETTING_METHOD_ID, cm.getId());

		List<Preference> prefsToSave = new ArrayList<Preference>();
		List<Preference> prefs = UserService.getInstance().getPreferences(PREF_TYPE_CM);
		String keyPrefix = "" + f.getId() + "#";
		String userName = SecurityService.getInstance().getCurrentUserName();
		for (String settingKey: settingsMap.keySet()) {
			Preference prefToUpdate = null;
			for (Preference p: prefs) {
				if (p.getItem().equals(keyPrefix + settingKey)) {
					prefToUpdate = p;
					break;
				}
			}
			if (prefToUpdate == null) {
				prefToUpdate = new Preference();
				prefToUpdate.setUser(userName);
				prefToUpdate.setType(PREF_TYPE_CM);
				prefToUpdate.setItem(keyPrefix + settingKey);
			}
			prefToUpdate.setValue(settingsMap.get(settingKey));
			prefsToSave.add(prefToUpdate);
		}

		UserService.getInstance().savePreferences(prefsToSave);
		getCache().put(getCacheKey(f), cm);
		ModelEventService.getInstance().fireEvent(new ModelEvent(f.getProtocolClass(), ModelEventType.ObjectChanged, 0));
	}

	public static void revertColorMethod(Feature f) {
		List<Preference> prefs = UserService.getInstance().getPreferences(PREF_TYPE_CM);
		String keyPrefix = "" + f.getId() + "#";
		for (Preference pref: prefs) {
			if (pref.getItem().startsWith(keyPrefix)) {
				UserService.getInstance().deletePreference(pref);
			}
		}

		getCache().remove(getCacheKey(f));
		ModelEventService.getInstance().fireEvent(new ModelEvent(f.getProtocolClass(), ModelEventType.ObjectChanged, 0));
	}

	/*
	 * Non-public
	 * **********
	 */

	private static ICache getCache() {
		return CacheService.getInstance().getDefaultCache();
	}
	
	private static String getCacheKey(Feature feature) {
		return "PersonalColorMethod#" + feature.getId();
	}
	
	private static String getPrefKey(Feature feature, String prefItem) {
		return feature.getId() + "#" + prefItem;
	}

	private static Map<String,String> getSettings(Feature f) {
		String settingKey = "" + f.getId() + "#";
		Map<String,String> settings = new HashMap<String, String>();
		List<Preference> prefs = UserService.getInstance().getPreferences(PREF_TYPE_CM);
		for (Preference p: prefs) {
			if (p.getItem().startsWith(settingKey)) {
				String key = p.getItem().substring(settingKey.length());
				String value = p.getValue();
				settings.put(key, value);
			}
		}
		return settings;
	}
}
