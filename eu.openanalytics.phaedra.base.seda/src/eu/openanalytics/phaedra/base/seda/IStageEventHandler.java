package eu.openanalytics.phaedra.base.seda;


public interface IStageEventHandler {

	/**
	 * Called when the stage starts running.
	 * A stage may start and stop running multiple times within the lifetime of an application.
	 */
	public void onStartup();
	
	/**
	 * Called when the stage stops running.
	 * A stage may start and stop running multiple times within the lifetime of an application.
	 * 
	 * @param forced True if the stage was forced to shut down without delay.
	 */
	public void onShutdown(boolean forced);
	
	/**
	 * Handle a stage event.
	 * 
	 * @param event The event to handle.
	 * @throws Exception If the event handling fails for any reason.
	 */
	public void handleEvent(StageEvent event) throws Exception;
	
	/**
	 * Handle an Exception that occurred during event handling.
	 * 
	 * @param event The event that was being handled.
	 * @param exception The exception that occurred.
	 */
	public void onEventException(StageEvent event, Throwable exception);
}
