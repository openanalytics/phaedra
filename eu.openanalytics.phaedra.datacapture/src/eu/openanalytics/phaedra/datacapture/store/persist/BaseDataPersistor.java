package eu.openanalytics.phaedra.datacapture.store.persist;

import java.io.IOException;
import java.util.Arrays;

import eu.openanalytics.phaedra.base.fs.store.IFileStore;

public abstract class BaseDataPersistor implements IDataPersistor {

	protected String[] getNames(IFileStore store, String prefix) throws IOException {
		return Arrays.stream(store.listKeys())
				.filter(key -> key.startsWith(prefix))
				.map(key -> key.substring(prefix.length()))
				.toArray(i -> new String[i]);
	}
	
}
