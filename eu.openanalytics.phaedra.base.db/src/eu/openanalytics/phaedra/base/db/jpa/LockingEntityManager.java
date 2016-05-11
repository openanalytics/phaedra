package eu.openanalytics.phaedra.base.db.jpa;

import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.metamodel.Metamodel;

import org.eclipse.persistence.internal.jpa.EntityManagerImpl;
import org.eclipse.persistence.sessions.Session;

/**
 * EntityManager whose methods use locking to avoid multiple threads
 * accessing the EntityManager simultaneously.
 * 
 * This locking does not handle the following scenarios, where the
 * callers must enable locking themselves (see getLock()):
 * 
 * <ul>
 * <li>Transactions obtained via getTransaction()</li>
 * <li>Queries obtained via createQuery() and similar methods</li>
 * </ul>
 */
public class LockingEntityManager implements EntityManager {

	private EntityManager wrappedManager;
	private Lock lock;
	
	public LockingEntityManager(EntityManager wrappedManager) {
		this.wrappedManager = wrappedManager;
		lock = new ReentrantLock();
	}
	
	public Lock getLock() {
		return lock;
	}
	
	public Session getActiveSession() {
		if (wrappedManager instanceof EntityManagerImpl) return ((EntityManagerImpl)wrappedManager).getActiveSession();
		return null;
	}
	
	@Override
	public void clear() {
		lock.lock();
		try {
			wrappedManager.clear();
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void close() {
		lock.lock();
		try {
			wrappedManager.close();
		} finally {
			lock.unlock();
		}
	}

	@Override
	public boolean contains(Object obj) {
		lock.lock();
		try {
			return wrappedManager.contains(obj);
		} finally {
			lock.unlock();
		}
	}

	@Override
	public Query createNamedQuery(String query) {
		lock.lock();
		try {
			return wrappedManager.createNamedQuery(query);
		} finally {
			lock.unlock();
		}
	}

	@Override
	public <T> TypedQuery<T> createNamedQuery(String query, Class<T> clazz) {
		lock.lock();
		try {
			return wrappedManager.createNamedQuery(query, clazz);
		} finally {
			lock.unlock();
		}
	}

	@Override
	public Query createNativeQuery(String query) {
		lock.lock();
		try {
			return wrappedManager.createNativeQuery(query);
		} finally {
			lock.unlock();
		}
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Query createNativeQuery(String query, Class clazz) {
		lock.lock();
		try {
			return wrappedManager.createNativeQuery(query, clazz);
		} finally {
			lock.unlock();
		}
	}

	@Override
	public Query createNativeQuery(String query, String arg1) {
		lock.lock();
		try {
			return wrappedManager.createNativeQuery(query, arg1);
		} finally {
			lock.unlock();
		}
	}

	@Override
	public Query createQuery(String query) {
		lock.lock();
		try {
			return wrappedManager.createQuery(query);
		} finally {
			lock.unlock();
		}
	}

	@Override
	public <T> TypedQuery<T> createQuery(CriteriaQuery<T> query) {
		lock.lock();
		try {
			return wrappedManager.createQuery(query);
		} finally {
			lock.unlock();
		}
	}

	@Override
	public <T> TypedQuery<T> createQuery(String arg0, Class<T> arg1) {
		lock.lock();
		try {
			return wrappedManager.createQuery(arg0, arg1);
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void detach(Object arg0) {
		lock.lock();
		try {
			wrappedManager.detach(arg0);
		} finally {
			lock.unlock();
		}
	}

	@Override
	public <T> T find(Class<T> arg0, Object arg1) {
		lock.lock();
		try {
			return wrappedManager.find(arg0, arg1);
		} finally {
			lock.unlock();
		}
	}

	@Override
	public <T> T find(Class<T> arg0, Object arg1, Map<String, Object> arg2) {
		lock.lock();
		try {
			return wrappedManager.find(arg0, arg1, arg2);
		} finally {
			lock.unlock();
		}
	}

	@Override
	public <T> T find(Class<T> arg0, Object arg1, LockModeType arg2) {
		lock.lock();
		try {
			return wrappedManager.find(arg0, arg1, arg2);
		} finally {
			lock.unlock();
		}
	}

	@Override
	public <T> T find(Class<T> arg0, Object arg1, LockModeType arg2,
			Map<String, Object> arg3) {
		lock.lock();
		try {
			return wrappedManager.find(arg0, arg1, arg2, arg3);
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void flush() {
		lock.lock();
		try {
			wrappedManager.flush();
		} finally {
			lock.unlock();
		}
	}

	@Override
	public CriteriaBuilder getCriteriaBuilder() {
		lock.lock();
		try {
			return wrappedManager.getCriteriaBuilder();
		} finally {
			lock.unlock();
		}
	}

	@Override
	public Object getDelegate() {
		lock.lock();
		try {
			return wrappedManager.getDelegate();
		} finally {
			lock.unlock();
		}
	}

	@Override
	public EntityManagerFactory getEntityManagerFactory() {
		lock.lock();
		try {
			return wrappedManager.getEntityManagerFactory();
		} finally {
			lock.unlock();
		}
	}

	@Override
	public FlushModeType getFlushMode() {
		lock.lock();
		try {
			return wrappedManager.getFlushMode();
		} finally {
			lock.unlock();
		}
	}

	@Override
	public LockModeType getLockMode(Object arg0) {
		lock.lock();
		try {
			return wrappedManager.getLockMode(arg0);
		} finally {
			lock.unlock();
		}
	}

	@Override
	public Metamodel getMetamodel() {
		lock.lock();
		try {
			return wrappedManager.getMetamodel();
		} finally {
			lock.unlock();
		}
	}

	@Override
	public Map<String, Object> getProperties() {
		lock.lock();
		try {
			return wrappedManager.getProperties();
		} finally {
			lock.unlock();
		}
	}

	@Override
	public <T> T getReference(Class<T> arg0, Object arg1) {
		lock.lock();
		try {
			return wrappedManager.getReference(arg0, arg1);
		} finally {
			lock.unlock();
		}
	}

	@Override
	public EntityTransaction getTransaction() {
		lock.lock();
		try {
			return wrappedManager.getTransaction();
		} finally {
			lock.unlock();
		}
	}

	@Override
	public boolean isOpen() {
		lock.lock();
		try {
			return wrappedManager.isOpen();
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void joinTransaction() {
		lock.lock();
		try {
			wrappedManager.joinTransaction();
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void lock(Object arg0, LockModeType arg1) {
		lock.lock();
		try {
			wrappedManager.lock(arg0, arg1);
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void lock(Object arg0, LockModeType arg1, Map<String, Object> arg2) {
		lock.lock();
		try {
			wrappedManager.lock(arg0, arg1, arg2);
		} finally {
			lock.unlock();
		}
	}

	@Override
	public <T> T merge(T arg0) {
		lock.lock();
		try {
			return wrappedManager.merge(arg0);
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void persist(Object arg0) {
		lock.lock();
		try {
			wrappedManager.persist(arg0);
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void refresh(Object arg0) {
		lock.lock();
		try {
			wrappedManager.refresh(arg0);
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void refresh(Object arg0, Map<String, Object> arg1) {
		lock.lock();
		try {
			wrappedManager.refresh(arg0, arg1);
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void refresh(Object arg0, LockModeType arg1) {
		lock.lock();
		try {
			wrappedManager.refresh(arg0, arg1);
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void refresh(Object arg0, LockModeType arg1, Map<String, Object> arg2) {
		lock.lock();
		try {
			wrappedManager.refresh(arg0, arg1, arg2);
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void remove(Object arg0) {
		lock.lock();
		try {
			wrappedManager.remove(arg0);
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void setFlushMode(FlushModeType arg0) {
		lock.lock();
		try {
			wrappedManager.setFlushMode(arg0);
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void setProperty(String arg0, Object arg1) {
		lock.lock();
		try {
			wrappedManager.setProperty(arg0, arg1);
		} finally {
			lock.unlock();
		}
	}

	@Override
	public <T> T unwrap(Class<T> arg0) {
		lock.lock();
		try {
			return wrappedManager.unwrap(arg0);
		} finally {
			lock.unlock();
		}
	}

}
