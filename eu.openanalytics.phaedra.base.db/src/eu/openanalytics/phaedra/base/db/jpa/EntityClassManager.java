package eu.openanalytics.phaedra.base.db.jpa;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;

import eu.openanalytics.phaedra.base.db.Activator;

/**
 * This class keeps track of all classes that represent managed entities.
 * Managed entities are registered via an extension point, and are
 * expected to implement {@link eu.openanalytics.phaedra.base.db.IValueObject}.
 */
public class EntityClassManager {

	public static final String MODEL_EXT_PT = Activator.PLUGIN_ID + ".persistenceModel";
	
	public static String[] getRegisteredEntityClassNames() {
		List<String> entities = new ArrayList<String>();
		IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(MODEL_EXT_PT);
		for (IConfigurationElement el : config) {
			IConfigurationElement[] children = el.getChildren("entityClass");
			if (children != null) {
				for (IConfigurationElement child: children) {
					String className = child.getAttribute("class");
					entities.add(className);
				}
			}
		}
		
		return entities.toArray(new String[entities.size()]);
	}
	
	public static Class<?>[] getRegisteredEntityClasses() {
		String[] classNames = getRegisteredEntityClassNames();
		Class<?>[] classes = new Class[classNames.length];
		for (int i = 0; i < classes.length; i++) {
			try {
				classes[i] = Class.forName(classNames[i]);
			} catch (ClassNotFoundException e) {}
		}
		return classes;
	}
}
