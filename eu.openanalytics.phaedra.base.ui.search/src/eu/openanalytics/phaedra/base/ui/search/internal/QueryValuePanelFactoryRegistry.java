package eu.openanalytics.phaedra.base.ui.search.internal;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;

import com.google.common.base.Objects;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import eu.openanalytics.phaedra.base.search.model.QueryFilter;
import eu.openanalytics.phaedra.base.ui.search.IQueryValuePanelFactory;

public class QueryValuePanelFactoryRegistry {
	private static final QueryValuePanelFactoryRegistry INSTANCE = new QueryValuePanelFactoryRegistry();
	
	private Map<String, IQueryValuePanelFactory> factories = new HashMap<>();
	
	private QueryValuePanelFactoryRegistry() {
		init();
	}

	private void init() {
		IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(IQueryValuePanelFactory.EXT_PT_ID);
		for (IConfigurationElement el : config) {
			try {
				Object o = el.createExecutableExtension(IQueryValuePanelFactory.ATTR_CLASS);
				if (o instanceof IQueryValuePanelFactory) {
					IQueryValuePanelFactory factory = (IQueryValuePanelFactory) o;
					factories.put(factory.getId(), factory);
				}
			} catch (CoreException e) {
				// Ignore invalid extensions.
			}
		}
		
	}
	
	public static QueryValuePanelFactoryRegistry getInstance() {
		return INSTANCE;
	}
	
	public IQueryValuePanelFactory getFactory(String id) {
		IQueryValuePanelFactory factory = factories.get(id);
		if (factory == null) {
			throw new IllegalArgumentException("No query value panel factory found for " + id);
		}
		return factory;
	}	
		
	/**
	 * Try to find a query panel factory for a filter.
	 * First tries to find a matching type, columnName operator type and operator (specific panels).
	 * If not found it tries to find a panel for an operator type and operator.
	 * When not found it tries to find a general panel with matching operator. 
	 * @param filter
	 * @return
	 */
	public IQueryValuePanelFactory getFactory(final QueryFilter filter) {
		if (filter.getOperator() == null) {
			return factories.get(EmptyQueryValuePanelFactory.class.getName());
		}		
		
		// try to find a specific factory for type, column, operator type and operator
		IQueryValuePanelFactory factory = Iterables.find(factories.values(), createPredicate(filter, true, true), null);
		
		// if not found, try to find a specific factory for operator type and operator
		if (factory == null) {
			factory = Iterables.find(factories.values(), createPredicate(filter, false, true), null);
		}
		
		// try to find a general factory for an operator
		if (factory == null) {
			factory = Iterables.find(factories.values(), createPredicate(filter, false, false), null);
		}
		
		if (factory == null) {
			throw new IllegalArgumentException("No query value panel factory found for query filter " + filter);
		}
		return factory;
	}
	
	private Predicate<IQueryValuePanelFactory> createPredicate(final QueryFilter filter, final boolean columnMatch, final boolean operatorTypeMatch) {
		return new Predicate<IQueryValuePanelFactory>() {
			@Override
			public boolean apply(IQueryValuePanelFactory factory) {
				return matches(factory, filter, columnMatch, operatorTypeMatch);
			}
		};
	}
	
	private boolean matches(final IQueryValuePanelFactory factory, final QueryFilter filter, final boolean columnMatch, final boolean operatorTypeMatch) {
		return Iterables.any(factory.getFilters(), new Predicate<QueryFilter>() {
			@Override
			public boolean apply(QueryFilter factoryFilter) {				
				if (columnMatch && !(Objects.equal(filter.getType(), factoryFilter.getType()) && Objects.equal(filter.getColumnName(), factoryFilter.getColumnName()))) {
					return false;
				}
				if (!columnMatch && (factoryFilter.getType() != null || factoryFilter.getColumnName() != null)) {
					return false;
				}				
				if (operatorTypeMatch && !Objects.equal(filter.getOperatorType(), factoryFilter.getOperatorType())) {
					return false;
				}
				if (!operatorTypeMatch && factoryFilter.getOperatorType() != null) {
					return false;
				}
				return Objects.equal(filter.getOperator(), factoryFilter.getOperator());
			}
		});
	}
	 
}
