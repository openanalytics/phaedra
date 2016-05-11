package eu.openanalytics.phaedra.base.ui.richtableviewer.state;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;

public class StateStoreRegistry {

	public final static String DEFAULT_STORE_ID = "default";
	
	private static Map<String, IStateStore> availableStores;
	private static IStateStore defaultStore;
	
	public static IStateStore getStore(String id) {
		if (availableStores == null) initializeStores();
		if (DEFAULT_STORE_ID.equals(id)) {
			return defaultStore;
		}
		return availableStores.get(id);
	}
	
	private static void initializeStores() {
		availableStores = new HashMap<String, IStateStore>();
		
		IConfigurationElement[] config = Platform.getExtensionRegistry()
				.getConfigurationElementsFor(IStateStore.EXT_PT_ID);
		for (IConfigurationElement el : config) {
			try {
				Object o = el.createExecutableExtension(IStateStore.ATTR_CLASS);
				if (o instanceof IStateStore) {
					IStateStore store = (IStateStore)o;
					String id = el.getAttribute(IStateStore.ATTR_ID);
					availableStores.put(id, store);
					
					// The first available store becomes the default store.
					if (defaultStore == null) defaultStore = store;
				}
			} catch (CoreException e) {
				// Invalid extension.
			}
		}
	}
}
