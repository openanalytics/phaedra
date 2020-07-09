package eu.openanalytics.phaedra.base.environment;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import org.apache.commons.lang3.tuple.Pair;

import eu.openanalytics.phaedra.base.db.jpa.BaseJPAService;
import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.base.security.model.Permissions;

public class GenericEntityService extends BaseJPAService {

	private static GenericEntityService instance = new GenericEntityService();
	
	public static GenericEntityService getInstance() {
		return instance;
	}
	
	private GenericEntityService() {
		// Hidden constructor
	}
	
	public <E> E findEntity(Class<E> entityClass, long id) {
		E entity = getEntity(entityClass, id);
		if (SecurityService.getInstance().check(Permissions.PROTOCOLCLASS_OPEN, entity)) return entity;
		else return null;
	}
	
	public <E> List<E> findEntities(Class<E> entityClass, long[] ids) {
		String queryString = String.format("select e from %s e where e.id in ?1", entityClass.getSimpleName());
		List<Long> idList = Arrays.stream(ids).mapToObj(l -> l).collect(Collectors.toList());
		return streamableList(getList(queryString, entityClass, Collections.singletonList(idList))).stream()
			.filter(e -> SecurityService.getInstance().check(Permissions.PROTOCOLCLASS_OPEN, e))
			.collect(Collectors.toList());
	}
	
	@SuppressWarnings("unchecked")
	public <T> List<T> executeQuery(String jpql, Pair<String,Object>... parameters) {
		return super.executeQuery(jpql, parameters);
	}
	
	public void refreshEntity(Object entity) {
		refresh(entity);
	}
	
	public <T> T runInLock(Callable<T> callable) {
		EntityManager em = getEntityManager();
		try {
			return callable.call();
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			releaseEntityManager(em);
		}
	}
}
