package eu.openanalytics.phaedra.base.seda;

public interface IStage {

	public final static String EXT_PT_ID = Activator.PLUGIN_ID + ".stage";
	public final static String ATTR_ID = "id";
	public final static String ATTR_EVENT_HANDLER = "eventHandler";
	public final static String ATTR_AUTOSTART = "autoStart";
	public final static String CONFIG_ELEMENT = "stageConfiguration";
	public final static String CONFIG_NAME = "name";
	public final static String CONFIG_VALUE = "value";
	
	/**
	 * Configure this stage. This method is called upon stage creation, and only once.
	 * 
	 * @param handler The handler that will handle incoming events.
	 * @param config The stage configuration.
	 */
	public void configure(IStageEventHandler handler, StageConfiguration config);
	
	/**
	 * Get the unique id of this stage.
	 * 
	 * @return The unique id of this stage.
	 */
	public String getId();
	
	/**
	 * Add a stage controller.
	 * Stage controllers can monitor the lifecycle of a stage,
	 * and influence its internal configuration.
	 * 
	 * @param controller The controller to add.
	 */
	public void addController(IStageController controller);
	
	/**
	 * Start running this stage.
	 * The stage will start polling its queue for new events.
	 */
	public void startup();
	
	/**
	 * Stop running this stage.
	 * Any active tasks will be interrupted, and new events
	 * will be refused.
	 * 
	 * @param force True to force the shutdown immediately.
	 * False to allow active tasks to finish before shutting down.
	 */
	public void shutdown(boolean force);
	
	/**
	 * Test whether this stage is currently running or not.
	 * A stage is running when the startup() method is called
	 * successfully, and stops running when the shutdown()
	 * method is called successfully.
	 * 
	 * @return True if the stage is currently running.
	 */
	public boolean isRunning();
	
	/**
	 * Submit an event to the queue of this stage.
	 * Do not call this method directly, use StageService.post(StageEvent) instead.
	 * 
	 * @param event The event to submit.
	 * @return True if the event was accepted, false otherwise.
	 */
	public boolean submit(StageEvent event);
	
	/**
	 * Cancel a queued event.
	 * If the specified in the event is not in the queue,
	 * this method does nothing (and returns false).
	 * 
	 * @param event The event to cancel.
	 * @return True if the event was cancelled.
	 */
	public boolean cancel(StageEvent event);
	
	/**
	 * Obtain an array containing all queued events.
	 * 
	 * @return An array containing all queued events.
	 */
	public StageEvent[] getQueuedEvents();
}
