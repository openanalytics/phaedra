package eu.openanalytics.phaedra.base.db.jpa;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import eu.openanalytics.phaedra.base.db.DatabaseConfig;

/**
 * A lazy provider for thread-local EntityManagers.
 * This should only be used in server-mode, i.e. when servicing requests on a per-thread basis.
 * When running Phaedra in client mode, use SessionEntityManager instead.
 */
public class ThreadLocalEntityManager {

	private static ThreadLocalEntityManager instance;

	private boolean isEnabled;
	private ThreadLocal<EntityManager> localEM;
	private ThreadLocal<Boolean> isLocalInitialized;
	
	private ThreadLocalEntityManager() {
		// Hidden constructor
	}
	
	public static ThreadLocalEntityManager getInstance() {
		return instance;
	}
	
	public static void initialize(DatabaseConfig cfg, EntityManagerFactory emFactory) {
		if (instance != null) throw new IllegalStateException("ThreadLocalEntityManager is already initialized");
		instance = new ThreadLocalEntityManager();
		
		instance.isEnabled = !Boolean.valueOf(cfg.get(DatabaseConfig.JPA_SESSION_EM, "true"));
		instance.localEM = new ThreadLocal<EntityManager>() {
			@Override
			protected EntityManager initialValue() {
//				EclipseLog.info("Instantiating new thread-local EntityManager for " + Thread.currentThread(), Activator.PLUGIN_ID);
				return emFactory.createEntityManager();
			}
		};
		instance.isLocalInitialized = new ThreadLocal<Boolean>() {
			@Override
			protected Boolean initialValue() {
				return false;
			}
		};
	}
	
	public boolean isEnabled() {
		return isEnabled;
	}
	
	public EntityManager getCurrent() {
		if (isEnabled) {
			isLocalInitialized.set(true);
			return localEM.get();
		}
		else return null;
	}
	
	public void closeCurrent() {
		if (isEnabled && isLocalInitialized.get()) {
//			EclipseLog.info("Closing threadlocal EntityManager for " + Thread.currentThread(), Activator.PLUGIN_ID);
			localEM.get().close();
			localEM.remove();
			isLocalInitialized.set(false);
		}
	};
}
