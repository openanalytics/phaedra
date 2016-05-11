package eu.openanalytics.phaedra.ui.plate.table;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import eu.openanalytics.phaedra.base.ui.nattable.state.IStatePersister;
import eu.openanalytics.phaedra.model.user.UserService;

/**
 * Persists NatTable state in a user-specific preference (in the database).
 */
public class NatTableStatePersister implements IStatePersister {

	private static ExecutorService backgroundSaver = Executors.newSingleThreadExecutor();
	
	@Override
	public void save(String key, Properties props) throws IOException {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		props.storeToXML(os, null);
		String xmlString = os.toString();
		backgroundSaver.submit(() -> {
			UserService.getInstance().setPreferenceValue("Memento", key, xmlString);
		});
	}

	@Override
	public boolean load(String key, Properties props) throws IOException {
		String value = UserService.getInstance().getPreferenceValue("Memento", key);
		if (value == null || value.isEmpty()) return false;
		props.loadFromXML(new ByteArrayInputStream(value.getBytes()));
		return true;
	}

}
