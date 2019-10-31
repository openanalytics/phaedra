package eu.openanalytics.phaedra.base.search;

import java.util.Set;

import javax.persistence.EntityManager;

import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.PlatformObject;

/**
 * Interface for factory for query builders. Since the query builders are statefull, they cannot be reused. 
 * So a factory is needed the generate query builders.
 */
public interface IQueryBuilderFactory extends IExecutableExtension {
	
	public final static String EXT_PT_ID = Activator.PLUGIN_ID + ".queryBuilderFactory";
	public final static String ATTR_CLASS = "class";
	public final static String ATTR_ID = "id";
	
	/**
	 * Returns the id of the query builder factory.
	 * @return
	 */
	public String getId();
	
	/**
	 * Returns the supported types by this query builder factory.
	 * @return
	 */
	public Set<Class<? extends PlatformObject>> getSupportedTypes();
	
	/**
	 * Returns a new builder for the given type.
	 * @param clazz
	 * @return
	 */
	public <T extends PlatformObject> IQueryBuilder<T> getBuilder(Class<T> clazz, EntityManager entityManager);
}
