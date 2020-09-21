package eu.openanalytics.phaedra.base.search;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.core.runtime.PlatformObject;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.ComparisonChain;

import eu.openanalytics.phaedra.base.db.jpa.BaseJPAService;
import eu.openanalytics.phaedra.base.search.internal.QueryBuilderFactoryRegistry;
import eu.openanalytics.phaedra.base.search.model.Operator;
import eu.openanalytics.phaedra.base.search.model.QueryException;
import eu.openanalytics.phaedra.base.search.model.QueryFilter;
import eu.openanalytics.phaedra.base.search.model.QueryModel;
import eu.openanalytics.phaedra.base.security.SecurityService;

/**
 * API to interact with queries. This includes:
 * <ul>
 * <li>Executing ad-hoc or saved queries</li>
 * <li>Saving, updating and deleting queries</li>
 * <li>Retrieving saved queries</li>
 * </ul>
 */
public final class SearchService extends BaseJPAService {
	
	private static final SearchService INSTANCE = new SearchService();
	
	private Comparator<QueryModel> queryComparator = new Comparator<QueryModel>() {
		@Override
		public int compare(QueryModel o1, QueryModel o2) {
			String type1 = o1.getType() == null ? null : o1.getType().getSimpleName();
			String type2 = o2.getType() == null ? null : o2.getType().getSimpleName();
			return ComparisonChain.start().compare(type1, type2).compare(o1.getName(), o2.getName()).result();
		}
	};
	
	private SearchService() {
	}
	
	public static SearchService getInstance() {
		return INSTANCE;
	}
	
	/**
	 * Creates a query builder for the given class.
	 * @param clazz
	 * @return
	 */
	public <T extends PlatformObject> IQueryBuilder<T> createQueryBuilder(Class<T> clazz, EntityManager em) {
		return getQueryBuilderFactory(clazz).getBuilder(clazz, em);
	}
	
	/**
	 * Returns a set of the supported classes for searching.
	 * @return
	 */
	public Set<Class<? extends PlatformObject>> getSupportedClasses() {
		return QueryBuilderFactoryRegistry.getInstance().getSupportedClasses();
	}
	
	/**
	 * Returns a sorted list of the names of the supported classes for searching. 
	 * @return
	 */
	public List<String> getSupportedClassNames() {
		Collection<String> supportedClassNames = Collections2.transform(getSupportedClasses(), new Function<Class<? extends PlatformObject>, String>() {
			@Override
			public String apply(Class<? extends PlatformObject> clazz) {
				return clazz.getSimpleName();
			}			
		});
		
		List<String> sortedNames = new LinkedList<>(supportedClassNames);
		Collections.sort(sortedNames);
		return Collections.unmodifiableList(sortedNames);
	}
	
	/**
	 * Searches the database using a QueryModel.
	 * @param queryModel
	 * @return
	 * @throws QueryException
	 */
	@SuppressWarnings("unchecked")
	public <T extends PlatformObject> List<T> search(QueryModel queryModel) throws QueryException {
		EntityManager em = getEntityManager();
		try {
			IQueryBuilder<T> queryBuilder = (IQueryBuilder<T>) createQueryBuilder(queryModel.getType(), em);
			queryBuilder.setQueryModel(queryModel);
			TypedQuery<T> query = queryBuilder.buildQuery();
			return query.getResultList();
		} finally {
			releaseEntityManager(em);
		}
	}
	
	public PlatformObject searchById(String typeName, long id) throws QueryException {
		Class<? extends PlatformObject> type = getSupportedClasses().stream().filter(c -> c.getSimpleName().equalsIgnoreCase(typeName)).findAny().orElse(null);
		if (type == null) return null;
		
		QueryFilter filter = new QueryFilter();
		filter.setType(type);
		filter.setColumnName("id");
		filter.setOperator(Operator.EQUALS);
		filter.setValue(id);
		
		QueryModel model = new QueryModel();
		model.setType(type);
		model.setMaxResults(1);
		model.addQueryFilter(filter);
		List<PlatformObject> results = search(model);
		
		if (results.isEmpty()) return null;
		else return results.get(0);
	}
	
	/**
	 * Returns a saved query by id.
	 * @param id
	 * @return
	 */
	public QueryModel getQueryModel(Long id) {
		return getEntity(QueryModel.class, id);
	}
	
	/**
	 * Saves a query to the database (insert or update).
	 * @param queryModel
	 */
	public void saveQuery(QueryModel queryModel) {
		queryModel.setDate(new Date());
		if (queryModel.getOwner() == null) {
			queryModel.setOwner(SecurityService.getInstance().getCurrentUserName());
		}
		save(queryModel);
	}
	
	/**
	 * Deletes a saved query from the database.
	 * @param queryModel
	 */
	public void deleteQuery(QueryModel queryModel) {
		delete(queryModel);
	}
	
	/**
	 * Refreshes a saved query to resync with the database.
	 * @param queryModel
	 */
	public void refreshQuery(QueryModel queryModel) {
		refresh(queryModel);
	}
	
	/**
	 * Returns a list of saved queries with the public flag set to <code>true</code> and example flag set to <code>false</code>.
	 */
	@SuppressWarnings("unchecked")
	public List<QueryModel> getPublicQueries() {
		List<QueryModel> resultList = executeNamedQuery(QueryModel.NAMED_QUERY_GET_PUBLIC_QUERIES);
		Collections.sort(resultList, queryComparator);
		return resultList;
	}
	
	/**
	 * Returns a list of saved queries with the public flag set to <code>false</code>, example flag set to <code>false</code> and owner set to the current user.
	 */
	@SuppressWarnings("unchecked")
	public List<QueryModel> getMyQueries() {
		List<QueryModel> resultList = executeNamedQuery(QueryModel.NAMED_QUERY_GET_MY_QUERIES,
				Pair.of("owner", SecurityService.getInstance().getCurrentUserName()));
		Collections.sort(resultList, queryComparator);
		return resultList;
	}

	/**
	 * Returns a list of saved queries with the example flag set to <code>true</code>.
	 */
	@SuppressWarnings("unchecked")
	public List<QueryModel> getExampleQueries() {
		List<QueryModel> resultList = executeNamedQuery(QueryModel.NAMED_QUERY_GET_EXAMPLE_QUERIES);
		Collections.sort(resultList, queryComparator);
		return resultList;
	}

	/**
	 * Returns the number of queries with the same name as the given name.
	 */
	@SuppressWarnings("unchecked")
	public int getSimilarQueryCount(String queryName) {
		return executeNamedQuery(QueryModel.NAMED_QUERY_GET_SIMILAR_QUERIES,
				Pair.of("name", queryName),
				Pair.of("owner", SecurityService.getInstance().getCurrentUserName())).size();
	}
	
	private IQueryBuilderFactory getQueryBuilderFactory(Class<? extends PlatformObject> clazz) {
		return (IQueryBuilderFactory) QueryBuilderFactoryRegistry.getInstance().getQueryBuilderFactory(clazz);
	}
}
