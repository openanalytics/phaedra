package eu.openanalytics.phaedra.datacapture.scanner.internal;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;

import eu.openanalytics.phaedra.datacapture.scanner.model.IScannerType;

public class ScannerTypeRegistry {

	private static Map<String,IScannerType> knownTypes;
	
	public static String[] getAvailableTypes() {
		if (knownTypes == null) loadScannerTypes();
		Set<String> ids = knownTypes.keySet();
		String[] idArray = new String[ids.size()];
		ids.toArray(idArray);
		Arrays.sort(idArray);
		return idArray;
	}
	
	public static IScannerType getScannerType(String id) {
		if (knownTypes == null) loadScannerTypes();
		return knownTypes.get(id);
	}
	
	private static void loadScannerTypes() {
		knownTypes = new HashMap<String, IScannerType>();
		
		IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(IScannerType.EXT_PT_ID);
		for (IConfigurationElement el : config) {
			String id = el.getAttribute(IScannerType.ATTR_ID);
			try {
				Object o = el.createExecutableExtension(IScannerType.ATTR_CLASS);
				if (o instanceof IScannerType) {
					knownTypes.put(id, (IScannerType)o);
				}
			} catch (CoreException e) {
				// Ignore invalid types.
			}
		}
	}
}
