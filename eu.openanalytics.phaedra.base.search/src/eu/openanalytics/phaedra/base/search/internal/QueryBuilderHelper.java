package eu.openanalytics.phaedra.base.search.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.criteria.From;
import javax.persistence.criteria.Join;

import org.eclipse.core.runtime.PlatformObject;

import com.google.common.collect.Ordering;

import eu.openanalytics.phaedra.base.search.IQueryBuilder;
import eu.openanalytics.phaedra.base.search.SearchService;
import eu.openanalytics.phaedra.base.search.model.QueryException;

public final class QueryBuilderHelper {	
	private QueryBuilderHelper() {		
	}
			
	/**
	 * Builds a path from the fromClass to the toClass by traversing parent relations. If more paths are found, the shortest path is chosen.
	 * @param queryBuilders
	 * @param list
	 * @param fromClass
	 * @param toClass
	 * @return
	 * @throws QueryException	When no path can be constructed.
	 */
	public static List<Class<? extends PlatformObject>> getParentClassesFromNodeToNode(Map<Class<? extends PlatformObject>, IQueryBuilder<? extends PlatformObject>> queryBuilders, List<Class<? extends PlatformObject>> list, Class<? extends PlatformObject> fromClass, Class<? extends PlatformObject> toClass) throws QueryException {		
		// calculate possible paths
		Set<List<Class<? extends PlatformObject>>> paths = getPathsFromNodeToNode(queryBuilders, fromClass, toClass);
		
		if (paths.isEmpty()) {
			throw new QueryException("Cannot build query: No relation found between " + toClass.getSimpleName() + " and " + fromClass.getSimpleName());
		}
		
		// only use shortest path
		Ordering<List<Class<? extends PlatformObject>>> ordering = new Ordering<List<Class<? extends PlatformObject>>>() {
			@Override
			public int compare(List<Class<? extends PlatformObject>> list1, List<Class<? extends PlatformObject>> list2) {
				return Integer.compare(list1.size(), list2.size());
			}
		};
		return ordering.min(paths);
	}
	
	/**
	 * Builds all paths from the fromClass to the toClass by traversing parent relations.
	 * @param queryBuilders
	 * @param fromNode
	 * @param toNode
	 * @return
	 */
	public static Set<List<Class<? extends PlatformObject>>> getPathsFromNodeToNode(Map<Class<? extends PlatformObject>, IQueryBuilder<? extends PlatformObject>> queryBuilders, Class<? extends PlatformObject> fromNode, Class<? extends PlatformObject> toNode) {
		IQueryBuilder<? extends PlatformObject> queryBuilder = queryBuilders.get(fromNode);
		
		if (fromNode.equals(toNode)) {
			Set<List<Class<? extends PlatformObject>>> paths = new HashSet<>();
			List<Class<? extends PlatformObject>> path = new ArrayList<>();
			paths.add(path);
			return paths;
		}
		
		Set<List<Class<? extends PlatformObject>>> paths = new HashSet<>();		
		for (Class<? extends PlatformObject> parentNode : queryBuilder.getDirectParentClasses()) {
			Set<List<Class<? extends PlatformObject>>> parentPaths = getPathsFromNodeToNode(queryBuilders, parentNode, toNode);
			for (List<Class<? extends PlatformObject>> path : parentPaths) {
				path.add(0, parentNode);
			}
			paths.addAll(parentPaths);
		}
		
		return paths;
	}
	
	/**
	 * Returns a map with the given query builder and its recursive parents
	 * @param queryBuilder
	 * @return
	 */
	public static Map<Class<? extends PlatformObject>, IQueryBuilder<? extends PlatformObject>> getParentQueryBuilders(IQueryBuilder<? extends PlatformObject> queryBuilder) {
		Map<Class<? extends PlatformObject>, IQueryBuilder<? extends PlatformObject>> builders = new HashMap<>();
		builders.put(queryBuilder.getType(), queryBuilder);
		for (Class<? extends PlatformObject> parentClass : queryBuilder.getDirectParentClasses()) {
			IQueryBuilder<? extends PlatformObject> parentQueryBuilder = SearchService.getInstance().createQueryBuilder(parentClass);			 
			builders.putAll(QueryBuilderHelper.getParentQueryBuilders(parentQueryBuilder));
		}
		return Collections.unmodifiableMap(builders);
	}
		
	/**
	 * Adds join nodes for inner joining using the criteria builder.
	 * @param queryBuilder
	 * @param parentQueryBuilders
	 * @param criteriaNodes
	 * @param classesToJoin
	 */
	public static void addJoinNodes(IQueryBuilder<? extends PlatformObject> queryBuilder, Map<Class<? extends PlatformObject>, IQueryBuilder<? extends PlatformObject>> parentQueryBuilders, final Map<Class<? extends PlatformObject>, From<? extends PlatformObject, ? extends PlatformObject>> criteriaNodes, final List<Class<? extends PlatformObject>> classesToJoin) {
		if (classesToJoin.isEmpty()) {
			return;
		}
		
		Class<? extends PlatformObject> parentClass = classesToJoin.get(0);
		IQueryBuilder<? extends PlatformObject> parentQueryBuilder = parentQueryBuilders.get(parentClass);
		if (!criteriaNodes.containsKey(parentClass)) {
			From<? extends PlatformObject, ? extends PlatformObject> from = criteriaNodes.get(queryBuilder.getType());
			Join<? extends PlatformObject, ? extends PlatformObject> join = null;
			try {
				join = from.join(parentQueryBuilder.getJoinProperty());
			} catch (IllegalArgumentException e) {
				join = from.join(parentQueryBuilder.getJoinProperty() + "s");
			}
			criteriaNodes.put(parentClass, join);
		}
		classesToJoin.remove(0);
	
		addJoinNodes(parentQueryBuilder, parentQueryBuilders, criteriaNodes, classesToJoin);		
	}
	
	public static void putQueryCriteriaNodes(Map<Class<? extends PlatformObject>, IQueryBuilder<? extends PlatformObject>> parentQueryBuilders, Map<Class<? extends PlatformObject>, From<? extends PlatformObject, ? extends PlatformObject>> criteriaNodes, IQueryBuilder<? extends PlatformObject> queryBuilder, List<Class<? extends PlatformObject>> classesToJoin) {
		for (Class<? extends PlatformObject> parentClass : queryBuilder.getDirectParentClasses()) {
			if (classesToJoin.contains(parentClass) && !criteriaNodes.containsKey(parentClass)) {
				IQueryBuilder<? extends PlatformObject> parentQueryBuilder = parentQueryBuilders.get(parentClass);
				From<? extends PlatformObject, ? extends PlatformObject> from = criteriaNodes.get(queryBuilder.getType());
				Join<? extends PlatformObject, ? extends PlatformObject> join = null;
				try {
					join = from.join(parentQueryBuilder.getJoinProperty());
				} catch (IllegalArgumentException e) {
					join = from.join(parentQueryBuilder.getJoinProperty() + "s");
				}
				criteriaNodes.put(parentClass, join);
				
				Collection<Class<? extends PlatformObject>> classesLeft = new ArrayList<>(classesToJoin);
				classesLeft.remove(parentClass);
				
				addJoinNodes(parentQueryBuilder, parentQueryBuilders, criteriaNodes, classesToJoin);
			}
		}
	}	
}	
	