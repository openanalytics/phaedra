package eu.openanalytics.phaedra.base.seda;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;

import eu.openanalytics.phaedra.base.seda.internal.SimpleStage;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;

/**
 * Entry point for working with stages.
 * <p>
 * A stage is a modular software component that has its own processing queue and threadpool.
 * It may interact with other stages by sending and receiving stage events, forming a staged network.
 * This is based on the SEDA architecture, for more information, see:
 * <a href="http://www.eecs.harvard.edu/~mdw/proj/seda">http://www.eecs.harvard.edu/~mdw/proj/seda</a>
 * </p><p>
 * Stages are registered via the stage extension point. They are started automatically unless
 * they have the property autoStart=false.
 * </p>
 */
public class StageService {

	private static StageService instance;
	
	private Map<String, IStage> stages;
	
	private StageService() {
		// Hidden constructor.
		loadStages();
	}
	
	public static synchronized StageService getInstance() {
		if (instance == null) instance = new StageService();
		return instance;
	}
	
	/**
	 * Retrieve the stage with the given id.
	 * 
	 * @param id The id of the requested stage.
	 * @return The stage, or null if no matching stage was found.
	 */
	public IStage getStage(String id) {
		return stages.get(id);
	}
	
	/**
	 * Post an event. The event describes the target stage.
	 * This method returns quickly, and is guaranteed to not throw any Exception.
	 * 
	 * @param event The event to post.
	 * @return True if the event was posted successfully. False if the event was refused,
	 * or if the target stage was not found or not running.
	 */
	public boolean post(StageEvent event) {
		IStage target = stages.get(event.targetStageId);
		if (target == null) {
			EclipseLog.warn("Discarded event to unknown stage: " + event.targetStageId, Activator.getDefault());
			return false;
		} else {
			try {
				return target.submit(event);
			} catch (Throwable t) {
				EclipseLog.error("Error delivering event to stage " + event.targetStageId + ": " + t.getMessage(), t, Activator.getDefault());
				return false;
			}
		}
	}
	
	/**
	 * Shut down the stage service.
	 * All running stages will be stopped. Incoming stage events will be rejected.
	 * 
	 * Note: the service cannot be restarted. Call this method only when the application exits.
	 * 
	 * @param force True to interrupt active stage events.
	 */
	public void shutdown(boolean force) {
		EclipseLog.info("Shutting down all stages", Activator.getDefault());
		for (IStage stage: stages.values()) {
			try {
				stage.shutdown(force);
			} catch (Throwable t) {
				EclipseLog.error("Error shutting down stage: " + t.getMessage(), t, Activator.getDefault());
			}
		}
	}
	
	/*
	 * Non-public
	 * **********
	 */
	
	private void loadStages() {
		stages = new HashMap<>();
		IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(IStage.EXT_PT_ID);
		for (IConfigurationElement el : config) {
			String id = el.getAttribute(IStage.ATTR_ID);
			try {
				Object o = el.createExecutableExtension(IStage.ATTR_EVENT_HANDLER);
				if (o instanceof IStageEventHandler) {
					IStageEventHandler handler = (IStageEventHandler)o;
					
					StageConfiguration stageConfig = new StageConfiguration();
					IConfigurationElement[] cfgs = el.getChildren(IStage.CONFIG_ELEMENT);
					for (IConfigurationElement cfg: cfgs) {
						String name = cfg.getAttribute(IStage.CONFIG_NAME);
						String value = cfg.getAttribute(IStage.CONFIG_VALUE);
						stageConfig.set(name, value);
					}
					
					IStage stage = createStage(id, handler, stageConfig);
					stages.put(id, stage);
					
					String autoStart = el.getAttribute(IStage.ATTR_AUTOSTART);
					if (autoStart == null || Boolean.valueOf(autoStart)) {
						try {
							stage.startup();
						} catch (Throwable t) {
							EclipseLog.error("Failed to start stage " + stage.getId() + ": " + t.getMessage(), t, Activator.getDefault());
						}
					}
				}
			} catch (CoreException e) {
				throw new IllegalArgumentException("Invalid stage: " + id);
			}
		}
	}
	
	private IStage createStage(String id, IStageEventHandler handler, StageConfiguration config) {
		IStage stage = new SimpleStage(id);
		stage.configure(handler, config);
		return stage;
	}
}
