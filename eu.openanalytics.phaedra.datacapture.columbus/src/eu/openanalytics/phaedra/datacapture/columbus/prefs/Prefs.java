package eu.openanalytics.phaedra.datacapture.columbus.prefs;

import java.util.Arrays;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.equinox.security.storage.StorageException;
import org.eclipse.jface.preference.IPreferenceStore;

import eu.openanalytics.phaedra.base.util.CollectionUtils;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.base.util.misc.StringUtils;
import eu.openanalytics.phaedra.datacapture.columbus.Activator;

public class Prefs extends AbstractPreferenceInitializer {

	private static final String HOST_NAME = "HOST_NAME";
	private static final String PORT_NR = "PORT_NR";
	private static final String USER_NAME = "USER_NAME";
	private static final String FILE_SHARE = "FILE_SHARE";
	
	private static final String COLUMBUS_IDS = "COLUMBUS_IDS";
	private static final String COLUMBUS_DEFAULT_ID = "COLUMBUS_DEFAULT_ID";
	
	private static final ColumbusLogin BLANK_LOGIN = new ColumbusLogin();
	
	@Override
	public void initializeDefaultPreferences() {
		// Nothing to do
	}
	
	public static String[] getLoginIds() {
		String ids = getStore().getString(COLUMBUS_IDS);
		if (ids.isEmpty()) return new String[] {};
		return ids.split(";");
	}
	
	public static ColumbusLogin getDefaultLogin() {
		String id = getStore().getString(COLUMBUS_DEFAULT_ID);
		ColumbusLogin defaultLogin = load(id);
		if (defaultLogin == null) defaultLogin = BLANK_LOGIN;
		return defaultLogin;
	}
	
	public static void setDefaultLoginId(String defaultLoginId) {
		if (defaultLoginId != null) {
			getStore().setValue(COLUMBUS_DEFAULT_ID, defaultLoginId);
		}
		else {
			getStore().setToDefault(COLUMBUS_DEFAULT_ID);
		}
	}
	
	public static IPreferenceStore getStore() {
		return Activator.getDefault().getPreferenceStore();
	}
	
	public static void save(ColumbusLogin login) {
		IPreferenceStore store = getStore();
		store.setValue(HOST_NAME + "_ID_" + login.id, login.host);
		store.setValue(PORT_NR + "_ID_" + login.id, login.port);
		store.setValue(FILE_SHARE + "_ID_" + login.id, login.fileShare);
		store.setValue(USER_NAME + "_ID_" + login.id, login.username);
		
		if (login.password != null && !login.password.isEmpty()) {
			ISecurePreferences preferences = SecurePreferencesFactory.getDefault();
			ISecurePreferences node = preferences.node(Activator.PLUGIN_ID);
			try {
				node.put(login.id, login.password, true);
			} catch (StorageException e) {
				EclipseLog.error("Failed to save Columbus password for " + login.id, e, Activator.getDefault());
			}
		}
		
		String[] ids = getLoginIds();
		// Remove invalid ids (e.g. if a login's id was renamed)
		if (CollectionUtils.contains(ids, login.id)) return;
		ids = Arrays.copyOf(ids, ids.length + 1);
		ids[ids.length-1] = login.id;
		getStore().setValue(COLUMBUS_IDS, StringUtils.createSeparatedString(ids, ";", false));
	}
	
	public static void remove(ColumbusLogin login) {
		IPreferenceStore store = getStore();
		store.setToDefault(HOST_NAME + "_ID_" + login.id);
		store.setToDefault(PORT_NR + "_ID_" + login.id);
		store.setToDefault(FILE_SHARE + "_ID_" + login.id);
		store.setToDefault(USER_NAME + "_ID_" + login.id);
		
		ISecurePreferences preferences = SecurePreferencesFactory.getDefault();
		ISecurePreferences node = preferences.node(Activator.PLUGIN_ID);
		node.remove(login.id);
		
		String[] ids = Arrays.stream(getLoginIds()).filter(id -> !id.equals(login.id)).toArray(i -> new String[i]);
		getStore().setValue(COLUMBUS_IDS, StringUtils.createSeparatedString(ids, ";", false));
	}

	public static ColumbusLogin load(String id) {
		if (id == null) return null;
		IPreferenceStore store = getStore();
		ColumbusLogin login = new ColumbusLogin();
		login.id = id;
		login.host = store.getString(HOST_NAME + "_ID_" + id);
		if (login.host.isEmpty()) return null;
		login.port = store.getInt(PORT_NR + "_ID_" + id);
		login.fileShare = store.getString(FILE_SHARE + "_ID_" + id);
		login.username = store.getString(USER_NAME + "_ID_" + id);
		
		ISecurePreferences preferences = SecurePreferencesFactory.getDefault();
		ISecurePreferences node = preferences.node(Activator.PLUGIN_ID);
		try {
			login.password = node.get(id, null);
		} catch (StorageException e) {
			EclipseLog.error("Failed to retrieve Columbus password for " + id, e, Activator.getDefault());
			return null;
		}
		return login;
	}
	
	public static class ColumbusLogin {
		
		public String id;
		public String host;
		public int port;
		public String username;
		public String password;
		public String fileShare;
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((id == null) ? 0 : id.hashCode());
			return result;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			ColumbusLogin other = (ColumbusLogin) obj;
			if (id == null) {
				if (other.id != null) return false;
			} else if (!id.equals(other.id))
				return false;
			return true;
		}
	}
}
