package eu.openanalytics.phaedra.base.search.internal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;

import eu.openanalytics.phaedra.base.search.ISecurityFilter;
import eu.openanalytics.phaedra.base.search.model.QueryFilter;

public final class SecurityFilterRegistry {
	private static final SecurityFilterRegistry INSTANCE = new SecurityFilterRegistry();
	
	private Map<String, ISecurityFilter> securityFilters = new HashMap<>();
	
	private SecurityFilterRegistry() {
		init();
	}

	private void init() {
		IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(ISecurityFilter.EXT_PT_ID);
		for (IConfigurationElement el : config) {
			try {
				Object o = el.createExecutableExtension(ISecurityFilter.ATTR_CLASS);
				if (o instanceof ISecurityFilter) {
					ISecurityFilter filter = (ISecurityFilter) o;
					securityFilters.put(filter.getId(), filter);
				}
			} catch (CoreException e) {
				// Ignore invalid extensions.
			}
		}
	}
		
	public static SecurityFilterRegistry getInstance() {
		return INSTANCE;
	}
	
	public ISecurityFilter getSecurityFilter(String id) {
		ISecurityFilter securityFilter = securityFilters.get(id);
		if (securityFilter == null) {
			throw new IllegalArgumentException("No query value panel factory found for " + id);
		}
		return securityFilter;
	}
	
	public Set<QueryFilter> getAllQueryFilters() {
		Set<QueryFilter> filters = new HashSet<>();
		for (ISecurityFilter securityFilter : securityFilters.values()) {
			filters.add(securityFilter.getInternalFilter());
		}
		return filters;		
	}
}
