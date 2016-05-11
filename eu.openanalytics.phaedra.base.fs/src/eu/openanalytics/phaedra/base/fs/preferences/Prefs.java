package eu.openanalytics.phaedra.base.fs.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import eu.openanalytics.phaedra.base.fs.Activator;


public class Prefs extends AbstractPreferenceInitializer {

	public final static String SMB_RESPONSE_TIMEOUT = "SMB_RESPONSE_TIMEOUT";
	public final static String SMB_SOCKET_TIMEOUT = "SMB_SOCKET_TIMEOUT";
	
	public final static String UPLOAD_RETRIES = "UPLOAD_RETRIES";
	public final static String UPLOAD_RETRY_DELAY = "UPLOAD_RETRY_DELAY";
	
	@Override
	public void initializeDefaultPreferences() {
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();

		store.setDefault(SMB_RESPONSE_TIMEOUT, 30000);
		store.setDefault(SMB_SOCKET_TIMEOUT, 60000);
		store.setDefault(UPLOAD_RETRIES, 10);
		store.setDefault(UPLOAD_RETRY_DELAY, 500);
	}
}
