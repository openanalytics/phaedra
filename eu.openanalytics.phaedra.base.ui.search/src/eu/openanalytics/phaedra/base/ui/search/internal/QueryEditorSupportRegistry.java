package eu.openanalytics.phaedra.base.ui.search.internal;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;

import eu.openanalytics.phaedra.base.ui.search.IQueryEditorSupport;

public class QueryEditorSupportRegistry {
	
	private static final QueryEditorSupportRegistry INSTANCE = new QueryEditorSupportRegistry();
	
	private Map<Class<?>, IQueryEditorSupport> factories = new HashMap<>();
	
	private QueryEditorSupportRegistry() {
		init();
	}
	
	private void init() {
		IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(IQueryEditorSupport.EXT_PT_ID);
		for (IConfigurationElement el : config) {
			try {
				Object o = el.createExecutableExtension(IQueryEditorSupport.ATTR_CLASS);
				if (o instanceof IQueryEditorSupport) {
					IQueryEditorSupport factory = (IQueryEditorSupport) o;
					factories.put(factory.getSupportedClass(), factory);
				}
			} catch (CoreException e) {
				// Ignore invalid extensions.
			}
		}
	}

	public static QueryEditorSupportRegistry getInstance() {
		return INSTANCE;
	}
	
	public IQueryEditorSupport getFactory(Class<?> clazz) {
		IQueryEditorSupport factory = factories.get(clazz);
		if (factory == null) {
			throw new IllegalArgumentException("No query editor support found for class " + clazz);
		}
		return factory;
	}
}
