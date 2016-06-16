package eu.openanalytics.phaedra.base.r.rservi;

import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;

import de.walware.ecommons.net.RMIRegistry;
import de.walware.rj.RjException;
import de.walware.rj.server.srvext.EServerUtil;
import de.walware.rj.server.srvext.ServerUtil;
import de.walware.rj.servi.RServi;
import de.walware.rj.servi.RServiUtil;
import de.walware.rj.servi.internal.PoolManager;
import de.walware.rj.servi.pool.PoolConfig;
import de.walware.rj.servi.pool.RServiImplE;
import de.walware.rj.servi.pool.RServiNodeConfig;
import de.walware.rj.servi.pool.RServiNodeFactory;

@SuppressWarnings("restriction")
public class RServiManager {

	private String name;
	private PoolManager rPoolManager;
	private int initialPoolSize;
	private Map<String,String> envVars;
	
	public RServiManager(final String appId, final Map<String,String> envVars) {
		this.name = appId;
		this.envVars = envVars;
	}
	
	public void setInitialPoolSize(int initialPoolSize) {
		this.initialPoolSize = initialPoolSize;
	}

	@SuppressWarnings("deprecation")
	public void setEmbedded(final String rHome) throws CoreException {
		try {
//			if (System.getSecurityManager() == null) {
//				if (System.getProperty("java.security.policy") == null) {
//					String policyFile = RServiImplE.getLocalhostPolicyFile();
//					System.setProperty("java.security.policy", policyFile);
//				}
//				System.setSecurityManager(new SecurityManager());
//			}
			final String[] libs = EServerUtil.searchRJLibsInPlatform(
					new String[] { ServerUtil.RJ_SERVER_ID, RServiUtil.RJ_SERVI_ID, RServiUtil.RJ_CLIENT_ID }, false);
			System.setProperty("java.rmi.server.codebase", ServerUtil.concatCodebase(libs));

			PatchedRMIUtil.INSTANCE.setEmbeddedPrivateMode(true);
			RMIRegistry registry = PatchedRMIUtil.INSTANCE.getEmbeddedPrivateRegistry(new NullProgressMonitor());
			RServiNodeFactory nodeFactory = RServiImplE.createLocalhostNodeFactory(name, registry);
			RServiNodeConfig rConfig = new RServiNodeConfig();
			rConfig.setRHome(rHome);
			rConfig.setEnableVerbose(true);
			rConfig.setEnableConsole(true);
			if (envVars != null) {
				for (String envVar: envVars.keySet()) {
					rConfig.getEnvironmentVariables().put(envVar, envVars.get(envVar));
				}
			}
			nodeFactory.setConfig(rConfig);
			
			rPoolManager = new PoolManager(name, registry);
			PoolConfig poolConfig = new PoolConfig();
			rPoolManager.setConfig(poolConfig);
			rPoolManager.addNodeFactory(nodeFactory);
			rPoolManager.init();
			
			// Make sure at least initialPoolSize nodes are started and ready.
			// This avoids having to wait for them to start when they are first called for.
			if (initialPoolSize < 1) initialPoolSize = 2;
			RServi[] pool = new RServi[initialPoolSize];
			for (int i=0; i<initialPoolSize; i++) {
				pool[i] = rPoolManager.getRServi("start"+i, null);
			}
			for (int i=0; i<initialPoolSize; i++) {
				pool[i].close();
			}
		}
		catch (RjException e) {
			stop();
			throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Embedded R instance could not created.", e));
		}
	}

	public RServi getRServi(String task) throws CoreException {
		RServi servi = null;
		try {
			servi = rPoolManager.getRServi(task, null);
		} catch (Exception e) {
			throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "No R instance available", e));
		}
		return servi;
	}
	
	public void stop() {
		if (rPoolManager != null) {
			try {
				rPoolManager.stop(0);
				rPoolManager = null;
				// PoolManager needs 1 sec to shut down its pool.
				// If we exit too soon, R instances (java.exe) will hang around.
				Thread.sleep(1200);
			} catch (Exception e) {}
		}
	}
	
}
