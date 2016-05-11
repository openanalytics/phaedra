package eu.openanalytics.phaedra.base.search;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.TypedQuery;

import org.eclipse.core.runtime.PlatformObject;

import eu.openanalytics.phaedra.base.search.model.QueryException;
import eu.openanalytics.phaedra.base.search.model.QueryModel;

/**
 * Interface for building a query.
 */
public interface IQueryBuilder<T extends PlatformObject> {
	/**
	 * Returns the query result type.  
	 * @return
	 */
	public Class<T> getType();
	
	/**
	 * Returns the name of the property that is used if other types have a relation to this type.
	 * @return
	 */
	public String getJoinProperty();
	
	/**
	 * Returns the set of parent classes which have a parent relationship to this object. 
	 * @return
	 */
	public Set<Class<? extends PlatformObject>> getDirectParentClasses();
	
	/**
	 * Returns a map of dependent builders that are needed for this query builder.
	 * @return
	 */
	public Map<Class<? extends PlatformObject>, IQueryBuilder<? extends PlatformObject>> getQueryBuilders();
	
	/**
	 * Returns a list of classes that define the path from a given parent type to this type.
	 * @param clazz
	 * @return
	 * @throws QueryException
	 */
	public List<Class<? extends PlatformObject>> getParentClassesToNode(Class<? extends PlatformObject> clazz) throws QueryException;
	
	/**
	 * Returns the query model used.
	 */
	public QueryModel getQueryModel();
	
	/**
	 * Sets the query model.
	 * @param queryModel
	 */
	public void setQueryModel(QueryModel queryModel);
	
	/**
	 * Builds the query model.
	 * @return
	 * @throws QueryException
	 */
	public TypedQuery<T> buildQuery() throws QueryException;

}
