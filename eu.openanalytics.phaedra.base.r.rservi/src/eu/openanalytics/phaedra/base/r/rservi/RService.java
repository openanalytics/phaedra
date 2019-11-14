package eu.openanalytics.phaedra.base.r.rservi;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.statet.rj.eclient.graphics.comclient.ERClientGraphicActionsFactory;
import org.eclipse.statet.rj.eclient.graphics.comclient.ERGraphicFactory;
import org.eclipse.statet.rj.server.RjsComConfig;
import org.eclipse.statet.rj.servi.RServi;
import org.eclipse.ui.PlatformUI;

import eu.openanalytics.phaedra.base.environment.prefs.Prefs;
import eu.openanalytics.phaedra.base.r.rservi.setup.RSetup;
import eu.openanalytics.phaedra.base.r.rservi.setup.RSetupUtil;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.base.util.process.ProcessUtils;

public class RService {

	public static final String R_SETUP_ID = "eu.openanalytics.phaedra.base.r.embeddedServer";
	
	private static RService instance = new RService();
	
	private RServiManager rServiManager;
	private boolean running;
	private Semaphore sessionLimiter;
	
	private RService() {
		createShutdownHook();
	}
	
	private void createShutdownHook() {
		Thread hook = new Thread() {
			@Override
			public void run() {
				instance.shutdown();
			}
		};
		Runtime.getRuntime().addShutdownHook(hook);
	}
	
	public static RService getInstance() {
		return instance;
	}
	
	public boolean isRunning() {
		return running;
	}
	
	public RServi createSession() throws CoreException {
		if (!running) throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "R is not running."));
		
		try {
			sessionLimiter.acquire();
		} catch (InterruptedException e) {
			throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Interrupted while waiting for an R session"));
		}
		return rServiManager.getRServi("EmbeddedR");
	}
	
	public void closeSession(RServi session) {
		try {
			session.close();
		} catch (CoreException e) {}
		sessionLimiter.release();
	}
	
	public void initialize() {
		if (running) throw new RuntimeException("Cannot initialize: R engine is already running");
		
		try {
			PlatformUI.getWorkbench();
			RjsComConfig.setProperty("rj.servi.graphicFactory", new ERGraphicFactory() {
				@Override
				public Map<String, ? extends Object> getInitServerProperties() {
					// Note: super.getInitServerProperties() performs a Display.syncExec to obtain display dpi.
					// This may cause deadlock, if the main thread is locked waiting for the sessionLimiter.
					// Since we should only use vector format graphics, dpi doesn't really matter anyway.
					Map<String, Object> map = new HashMap<>();
					map.put("display.dpi", new double[] { 96.0, 96.0 });
					return map;
				}
			});
			RjsComConfig.setProperty("rj.servi.comClientGraphicActionsFactory", new ERClientGraphicActionsFactory());
		} catch (IllegalStateException e) {
			// No workbench available: continue without ERGraphics.
		}
		
		// Get maximum pool size from preferences.
		int poolSize = eu.openanalytics.phaedra.base.environment.Activator.getDefault().getPreferenceStore().getInt(Prefs.R_POOL_SIZE);
		sessionLimiter = new Semaphore(poolSize);

		RSetup setup = RSetupUtil.loadSetup(R_SETUP_ID, null);
		if (setup != null) {
			try {
				Map<String,String> extraVars = new HashMap<String,String>();
				
				// Adjust PATH to include R binary path.
				String rHome = setup.getRHome();
				String binPath = rHome+"/bin";	
				if (ProcessUtils.isWindows()) binPath += (setup.getOSArch().contains("64") ? "/x64" : "/i386");
				String rNodePath = binPath + File.pathSeparator + System.getenv("PATH");
				extraVars.put("PATH", rNodePath);
				
				// Add R_LIBS_USER referring to all known lib paths.
				StringBuilder libString = new StringBuilder();
				List<String> userLibs = setup.getRLibsUser();
				for (String lib: userLibs) {
					libString.append(lib + File.pathSeparator);
				}
				extraVars.put("R_LIBS_USER", libString.toString());
				
				// Start the RServi manager.
				rServiManager = new RServiManager("Phaedra", extraVars);
				rServiManager.setInitialPoolSize(Math.max(2, poolSize));
				rServiManager.setEmbedded(rHome);
				running = true;
			} catch (Exception e) {
				EclipseLog.error("Error starting R Servi", e, Activator.getDefault());
			}
		}
	}
	
	public void shutdown() {
		if (rServiManager != null) rServiManager.stop();
		running = false;
	}
}
