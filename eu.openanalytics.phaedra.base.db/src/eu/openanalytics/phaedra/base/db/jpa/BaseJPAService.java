package eu.openanalytics.phaedra.base.db.jpa;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import org.eclipse.persistence.indirection.IndirectList;

import eu.openanalytics.phaedra.base.db.JDBCUtils;

/**
 * Common base class for data access services.
 * 
 * It provides a number of convenience methods for JPA entity interaction
 * such as select, update, delete.
 * 
 * All of these methods use locking to avoid multithreaded EntityManager
 * access, because EntityManager is not threadsafe.
 */
public abstract class BaseJPAService {

	/**
	 * Get the EntityManager this service should use for all its operations.
	 * 
	 * @return The EntityManager to use.
	 */
	protected abstract EntityManager getEntityManager();
	
	/**
	 * Get an entity based on its primary key.
	 * The class lookup is based on Eclipse-BuddyPolicy.
	 * 
	 * @param entityClass The name of the class of the entity to get.
	 * @param id The primary key.
	 * @return The matching entity or null if not found.
	 */
	@SuppressWarnings("unchecked")
	protected <E> E getEntity(String entityClass, Object id) {
		try {
			return getEntity((Class<E>) Class.forName(entityClass), id);
		} catch (ClassNotFoundException e) {
			return null;
		}
	}
	
	/**
	 * Get an entity based on its primary key.
	 * 
	 * @param entityClass The class of the entity to get.
	 * @param id The primary key.
	 * @return The matching entity or null if not found.
	 */
	protected <E> E getEntity(Class<E> entityClass, Object id) {
		EntityManager em = getEntityManager();
		E entity = em.find(entityClass, id);
		return entity;
	}
	
	/**
	 * Get a single entity based on a query.
	 * 
	 * @param jpql The query to use.
	 * @param entityClass The class of the entity.
	 * @param params Optional query arguments.
	 * @return The matching entity. Null if none found.
	 * If multiple results found, throws a NonUniqueResultException.
	 */
	protected <E> E getEntity(String jpql, Class<E> entityClass, Object... params) {
		EntityManager em = getEntityManager();
		TypedQuery<E> typedQuery = em.createQuery(jpql, entityClass);
		for (int i=1; i<=params.length; i++) {
			typedQuery.setParameter(i, params[i-1]);
		}
		try {
			return typedQuery.getSingleResult();
		} catch (NoResultException e) {
			return null;
		}
	}
	
	/**
	 * Get a List of all entities of a class.
	 *  
	 * @param returnClass The class of the entities.
	 * @return A List of matching entities.
	 */
	protected <E> List<E> getList(Class<E> returnClass) {
		EntityManager em = getEntityManager();
		TypedQuery<E> typedQuery = em.createQuery(
				"select o from " + returnClass.getSimpleName() + " o", returnClass);
		JDBCUtils.lockEntityManager(em);
		try {
			return typedQuery.getResultList();
		} finally {
			JDBCUtils.unlockEntityManager(em);	
		}
	}
	
	/**
	 * Get a List of entities using a query.
	 * 
	 * @param jpql The query to use.
	 * @param returnClass The class of the entities.
	 * @param param A non-null list of parameters.
	 * @return A List of matching entities.
	 */
	protected <E> List<E> getList(String jpql, Class<E> returnClass, List<?> params) {
		EntityManager em = getEntityManager();
		TypedQuery<E> typedQuery = em.createQuery(jpql, returnClass);
		for (int i=1; i<=params.size(); i++) {
			typedQuery.setParameter(i, params.get(i-1));
		}
		JDBCUtils.lockEntityManager(em);
		try {
			return typedQuery.getResultList();
		} finally {
			JDBCUtils.unlockEntityManager(em);	
		}
	}
	
	/**
	 * Get a List of entities using a query.
	 * 
	 * @param jpql The query to use.
	 * @param returnClass The class of the entities.
	 * @param param An optional series of parameters. 
	 * @return A List of matching entities.
	 */
	protected <E> List<E> getList(String jpql, Class<E> returnClass, Object... params) {
		EntityManager em = getEntityManager();
		TypedQuery<E> typedQuery = em.createQuery(jpql, returnClass);
		for (int i=1; i<=params.length; i++) {
			typedQuery.setParameter(i, params[i-1]);
		}
		JDBCUtils.lockEntityManager(em);
		try {
			return typedQuery.getResultList();
		} finally {
			JDBCUtils.unlockEntityManager(em);
		}
	}
	
	/**
	 * Save (create or update) an entity.
	 * 
	 * @param The entity to save.
	 */
	protected void save(Object o) {
		beforeSave(o);
		EntityManager em = getEntityManager();
		JDBCUtils.lockEntityManager(em);
		try {
			em.getTransaction().begin();
			em.persist(o);
			em.flush();
			em.getTransaction().commit();
		} finally {
			if (em.getTransaction().isActive()) em.getTransaction().rollback();
			JDBCUtils.unlockEntityManager(em);
		}
		afterSave(o);
	}
	
	protected void beforeSave(Object o) {
		// Default behaviour: do nothing.
	}
	
	protected void afterSave(Object o) {
		// Default behaviour: do nothing.
	}
	
	/**
	 * Save (create or update) a Collection of entities.
	 * 
	 * @param list The entities to save.
	 */
	protected void saveCollection(Collection<?> items) {
		beforeSave(items);
		EntityManager em = getEntityManager();
		JDBCUtils.lockEntityManager(em);
		try {
			em.getTransaction().begin();
			for (Object o: items) em.persist(o);
			em.flush();
			em.getTransaction().commit();
		} finally {
			if (em.getTransaction().isActive()) em.getTransaction().rollback();
			JDBCUtils.unlockEntityManager(em);
		}
		afterSave(items);
	}
	
	/**
	 * Delete an entity.
	 * 
	 * @param o The entity to delete.
	 */
	protected void delete(Object o) {
		beforeDelete(o);
		EntityManager em = getEntityManager();
		JDBCUtils.lockEntityManager(em);
		try {
			em.getTransaction().begin();
			em.remove(o);
			em.getTransaction().commit();
		} finally {
			if (em.getTransaction().isActive()) em.getTransaction().rollback();
			JDBCUtils.unlockEntityManager(em);
		}
		afterDelete(o);
	}
	
	protected void beforeDelete(Object o) {
		// Default behaviour: do nothing.
	}
	
	protected void afterDelete(Object o) {
		// Default behaviour: do nothing.
	}
	
	/**
	 * Execute an UPDATE or DELETE jpql statement.
	 * 
	 * @param jpql The statement to execute.
	 * @param params The parameters to add to the statement.
	 * @return The number of rows deleted.
	 */
	protected int executeUpdate(String jpql, Object... params) {
		EntityManager em = getEntityManager();
		JDBCUtils.lockEntityManager(em);
		try {
			Query query = em.createQuery(jpql);
			for (int i=1; i<=params.length; i++) {
				query.setParameter(i, params[i-1]);
			}
			return query.executeUpdate();
		} finally {
			JDBCUtils.unlockEntityManager(em);
		}
	}

	/**
	 * Since EclipseLink may return IndirectLists (lazy lists) which are not
	 * stream-compatible, use this method to ensure you're working with
	 * a stream-compatible List.
	 * 
	 * @param list The List that may or may not be stream-compatible.
	 * @return A stream-compatible List.
	 */
	public static <E> List<E> streamableList(List<E> list) {
		if (list instanceof IndirectList) return new ArrayList<>(list);
		return list;
	}
}
