package eu.openanalytics.phaedra.base.search.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.PlatformObject;

import eu.openanalytics.phaedra.base.search.IQueryBuilderFactory;

public final class QueryBuilderFactoryRegistry {
	private static final QueryBuilderFactoryRegistry INSTANCE = new QueryBuilderFactoryRegistry();
	
	private Map<Class<? extends PlatformObject>, IQueryBuilderFactory> queryBuilderFactories = new HashMap<>();
	
	private QueryBuilderFactoryRegistry() {
		loadQueryBuilderFactories();
	}
	
	public static QueryBuilderFactoryRegistry getInstance() {
		return INSTANCE;
	}
	
	private void loadQueryBuilderFactories() {
		IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(IQueryBuilderFactory.EXT_PT_ID);
		for (IConfigurationElement el : config) {
			try {
				Object o = el.createExecutableExtension(IQueryBuilderFactory.ATTR_CLASS);
				if (o instanceof IQueryBuilderFactory) {
					IQueryBuilderFactory queryBuilderFactory = (IQueryBuilderFactory) o;
					for (Class<? extends PlatformObject> type : queryBuilderFactory.getSupportedTypes()) {
						queryBuilderFactories.put(type, queryBuilderFactory);
					}
				}
			} catch (CoreException e) {
				// Ignore invalid extensions.
			}
		}
	}

	public IQueryBuilderFactory getQueryBuilderFactory(final Class<? extends PlatformObject> clazz) {
		IQueryBuilderFactory factory = queryBuilderFactories.get(clazz);
		if (factory == null) {
			throw new IllegalArgumentException("Unsupported class: No QueryBuilderFactory found for " + clazz);
		}
		return factory;
	}
	
	public Set<Class<? extends PlatformObject>> getSupportedClasses() {
		return queryBuilderFactories.keySet();
	}
}
