package eu.openanalytics.phaedra.base.r.rservi;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.statet.ecommons.rmi.core.RMIRegistry;
import org.eclipse.statet.internal.rj.servi.PoolManager;
import org.eclipse.statet.rj.RjException;
import org.eclipse.statet.rj.server.osgi.ERJContext;
import org.eclipse.statet.rj.server.util.ServerUtils;
import org.eclipse.statet.rj.servi.RServi;
import org.eclipse.statet.rj.servi.RServiUtil;
import org.eclipse.statet.rj.servi.node.RServiImpl;
import org.eclipse.statet.rj.servi.node.RServiNodeConfig;
import org.eclipse.statet.rj.servi.node.RServiNodeFactory;
import org.eclipse.statet.rj.servi.pool.PoolConfig;

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

	public void setEmbedded(final String rHome) throws CoreException {
		try {
//			if (System.getSecurityManager() == null) {
//				if (System.getProperty("java.security.policy") == null) {
//					String policyFile = RServiImplE.getLocalhostPolicyFile();
//					System.setProperty("java.security.policy", policyFile);
//				}
//				System.setSecurityManager(new SecurityManager());
//			}
			
			ERJContext context = new ERJContext();
			List<String> libs = context.searchRJLibs(Arrays.asList(new String[] {
					ServerUtils.RJ_SERVER_ID, ServerUtils.RJ_DATA_ID,
					RServiUtil.RJ_SERVI_ID, RServiUtil.RJ_CLIENT_ID,
					"org.eclipse.statet.rj.services.core" }));
			System.setProperty("java.rmi.server.codebase", ServerUtils.concatCodebase(libs));

			PatchedRMIUtil.INSTANCE.setEmbeddedPrivateMode(true);
			
			//TODO Make port (default: 51234) configurable: PatchedRMIUtil.INSTANCE.setEmbeddedPrivatePort(port);
			// But each node also opens 1 or 2 random ports! cfr UnicastRemoteObject.exportObject
			
			RMIRegistry registry = PatchedRMIUtil.INSTANCE.getEmbeddedPrivateRegistry(new NullProgressMonitor());
			RServiNodeFactory nodeFactory = RServiImpl.createLocalNodeFactory(name, context);
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
			nodeFactory.setRegistry(registry);
			
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
