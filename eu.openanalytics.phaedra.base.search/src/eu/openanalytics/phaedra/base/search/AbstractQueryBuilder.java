package eu.openanalytics.phaedra.base.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;

import org.eclipse.core.runtime.PlatformObject;

import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.Sets;

import eu.openanalytics.phaedra.base.search.internal.QueryBuilderHelper;
import eu.openanalytics.phaedra.base.search.internal.SecurityFilterRegistry;
import eu.openanalytics.phaedra.base.search.model.QueryException;
import eu.openanalytics.phaedra.base.search.model.QueryFilter;
import eu.openanalytics.phaedra.base.search.model.QueryModel;
import eu.openanalytics.phaedra.base.search.model.QueryOrdering;

public abstract class AbstractQueryBuilder<T extends PlatformObject> implements IQueryBuilder<T> {
	
	private QueryModel queryModel;
	private EntityManager entityManager;
	private CriteriaBuilder criteriaBuilder;
	private CriteriaQuery<T> criteriaQuery;
	private Map<Class<? extends PlatformObject>, IQueryBuilder<? extends PlatformObject>> queryBuilders;
	private Map<Class<? extends PlatformObject>, From<? extends PlatformObject, ? extends PlatformObject>> criteriaNodes = new HashMap<>();

	public AbstractQueryBuilder(EntityManager entityManager) {
		this.entityManager = entityManager;
		this.criteriaBuilder = entityManager.getCriteriaBuilder();
		this.criteriaQuery = criteriaBuilder.createQuery(getType());
		this.criteriaNodes.put(getType(), criteriaQuery.from(getType()));
	}
	
	@Override
	public QueryModel getQueryModel() {
		return queryModel;
	}
	
	@Override
	public void setQueryModel(QueryModel queryModel) {
		this.queryModel = queryModel;
	}
		
	/**
	 * Returns a map with the current query builder and all recursive parent query builders.
	 * @return
	 */
	@Override
	public Map<Class<? extends PlatformObject>, IQueryBuilder<? extends PlatformObject>> getQueryBuilders() {
		if (queryBuilders == null) {
			this.queryBuilders = QueryBuilderHelper.getParentQueryBuilders(this, entityManager);
		}		
		return this.queryBuilders;
	}
	
	@Override
	public List<Class<? extends PlatformObject>> getParentClassesToNode(Class<? extends PlatformObject> clazz) throws QueryException {		
		return QueryBuilderHelper.getParentClassesFromNodeToNode(getQueryBuilders(), new ArrayList<Class<? extends PlatformObject>>(), getType(), clazz);
	}
	
	private com.google.common.base.Predicate<QueryFilter> buildPredicate(final Collection<Class<? extends PlatformObject>> types) {
		return new com.google.common.base.Predicate<QueryFilter>() {
			@Override
			public boolean apply(QueryFilter queryFilter) {
				return types.contains(queryFilter.getType());
			}
		};
	}
	
	@SafeVarargs
	private final Collection<QueryFilter> getQueryFilters(boolean include, Class<? extends PlatformObject>... types) {
		com.google.common.base.Predicate<QueryFilter> predicate = buildPredicate(Arrays.asList(types));
		return Collections2.filter(queryModel.getQueryFilters(), include ? predicate : Predicates.not(predicate));
	}

	@SafeVarargs
	private final Collection<QueryFilter> getQueryFilters(final Class<? extends PlatformObject>... types) {
		return getQueryFilters(true, types);
	}
	
	@SuppressWarnings("unchecked")
	private Set<Class<? extends PlatformObject>> getParentClassesInQueryFilters(boolean includeCurrent) {		
		// find the parent classes that are explicitly mentioned
		Set<Class<? extends PlatformObject>> parentQueryBuilder = new HashSet<>(getQueryBuilders().keySet());
		if (!includeCurrent) {
			parentQueryBuilder.remove(getType());		
		}
		return getTypes(getQueryFilters(parentQueryBuilder.toArray(new Class[0])));
	}
	
	private HashSet<Class<? extends PlatformObject>> getTypes(Collection<QueryFilter> queryFilters) {
		return new HashSet<Class<? extends PlatformObject>>(Collections2.transform(queryFilters, new Function<QueryFilter, Class<? extends PlatformObject>>() {
			@Override
			public Class<? extends PlatformObject> apply(QueryFilter queryFilter) {
				return queryFilter.getType();
			}
		}));
	}
	
	/**
	 * Builds a query using the following logic
	 * <li>First the applicable security filters are looked up. Each applicable security filter is added to the list of filters. 
	 * So it is an internal filter, invisible to the user and transparent for the framework.
	 * <li>From the result type the parent types are looked up. These are the types that will result in inner joins with the result type.
	 * <li>Then predicates are generated form the (inner join) filters.
	 * <li>For each filter for which the type is a child of the result type a new predicate is added which will result in a subquery with an exists predicate.
	 * <li>If applicable ordering is applied on some fields of the result type.
	 * <li>Finally the query is generated an the maximum results is set.
	 * @return a typed query.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public TypedQuery<T> buildQuery() throws QueryException {
		// find securityFilters which are applicable
		Set<QueryFilter> securityFilters = Sets.filter(SecurityFilterRegistry.getInstance().getAllQueryFilters(), new com.google.common.base.Predicate<QueryFilter>() {
			@Override
			public boolean apply(QueryFilter securityFilter) {
				return !QueryBuilderHelper.getPathsFromNodeToNode(queryBuilders, getType(), securityFilter.getType()).isEmpty();
			}
		});
		
		// add inner joins needed for query filters and security filters
		Set<Class<? extends PlatformObject>> joinTypes = Sets.union(getParentClassesInQueryFilters(false), getTypes(securityFilters));
		for (Class<? extends PlatformObject> parentClass : joinTypes) {
			QueryBuilderHelper.addJoinNodes(this, queryBuilders, criteriaNodes, getParentClassesToNode(parentClass));
		}
		
		Set<Predicate> predicates = new HashSet<>();
		
		Class<T>[] classesWithCurrent = getQueryBuilders().keySet().toArray(new Class[0]);
		
		// filter part
		for (QueryFilter queryFilter : getQueryFilters(classesWithCurrent)) {
			predicates.add(queryFilter.getOperator().execute(criteriaBuilder, criteriaNodes.get(queryFilter.getType()), queryFilter.getColumnName(), queryFilter.isPositive(), queryFilter.isCaseSensitive(), queryFilter.getValue()));
		}

		// add security filters
		for (QueryFilter queryFilter : securityFilters) {
			predicates.add(queryFilter.getOperator().execute(criteriaBuilder, criteriaNodes.get(queryFilter.getType()), queryFilter.getColumnName(), queryFilter.isPositive(), queryFilter.isCaseSensitive(), queryFilter.getValue()));
		}		

		// subquery part
		for (QueryFilter queryFilter : getQueryFilters(false, classesWithCurrent)) {
			Map<Class<? extends PlatformObject>, From<? extends PlatformObject, ? extends PlatformObject>> criteriaNodes = new HashMap<>();
			IQueryBuilder<? extends PlatformObject> subqueryBuilder = SearchService.getInstance().createQueryBuilder(queryFilter.getType(), entityManager);
			
			List<Class<? extends PlatformObject>> classesToJoin = subqueryBuilder.getParentClassesToNode(getType());

			Subquery<? extends PlatformObject> subquery = criteriaQuery.subquery(subqueryBuilder.getType());
			Root<? extends PlatformObject> fromNode = subquery.from(subqueryBuilder.getType());
			criteriaNodes.put(subqueryBuilder.getType(), fromNode);					
			QueryBuilderHelper.putQueryCriteriaNodes(subqueryBuilder.getQueryBuilders(), criteriaNodes, subqueryBuilder, classesToJoin);
				
			Predicate subqueryPredicate = queryFilter.getOperator().execute(criteriaBuilder, fromNode, queryFilter.getColumnName(), queryFilter.isPositive(), queryFilter.isCaseSensitive(), queryFilter.getValue());
			Predicate correlatePredicate = criteriaBuilder.equal(this.criteriaNodes.get(getType()), criteriaNodes.get(getType()));
			
			subquery.where(criteriaBuilder.and(subqueryPredicate, correlatePredicate));
			predicates.add(criteriaBuilder.exists(subquery));
		}
		
		// create where part using predicates
		criteriaQuery.where(criteriaBuilder.and(predicates.toArray(new Predicate[0])));
		
		// create ordering part
		if (queryModel.getQueryOrderings().size() > 0) {
			From<? extends PlatformObject, ? extends PlatformObject> from = criteriaNodes.get(getType());
			List<Order> orders = new ArrayList<>();
			for (QueryOrdering queryOrdering : queryModel.getQueryOrderings()) {
				if (!queryOrdering.isCaseSensitive() && queryOrdering.getColumnType().equals(String.class)) {
					orders.add(queryOrdering.isAscending() 
						? criteriaBuilder.asc(criteriaBuilder.lower(from.<String>get(queryOrdering.getColumnName())))
						: criteriaBuilder.desc(criteriaBuilder.lower(from.<String>get(queryOrdering.getColumnName()))));
				} else {
					orders.add(queryOrdering.isAscending() 
						? criteriaBuilder.asc(from.get(queryOrdering.getColumnName()))
						: criteriaBuilder.desc(from.get(queryOrdering.getColumnName())));
				}
			}
			criteriaQuery.orderBy(orders);
		}
		
		// build query and set max number of results
		TypedQuery<T> query = entityManager.createQuery(criteriaQuery);
		if (queryModel.isMaxResultsSet()) {
			query.setMaxResults(queryModel.getMaxResults());
		}
		
		return query;		
	}
}
