package eu.openanalytics.phaedra.base.seda;

public interface IStageController {

	/**
	 * Get the ID of this controller.
	 * The controller ID must be unique within the stage.
	 * 
	 * @return The ID of the controller.
	 */
	public String getId();
	
	/**
	 * Initialize the controller.
	 * This happens as soon as a controller is attached to a stage.
	 * The stage may or may not be running at that point.
	 * 
	 * @param stage The stage to which the controller is attached.
	 * @param stageInternals The internal state of the stage.
	 */
	public void initialize(IStage stage, StageInternals stageInternals);
	
	/**
	 * Hook when the stage is started up.
	 */
	public void stageStarted();
	
	/**
	 * Hook when the stage is stopped.
	 */
	public void stageStopped();
	
	/**
	 * Hook when an event is submitted to the stage.
	 * Only called when the event has been accepted by the queue.
	 * 
	 * @param event The event that was submitted.
	 */
	public void eventSubmitted(StageEvent event);
	
	/**
	 * Hook when an event is about to be executed.
	 * 
	 * @param event The event that will be executed.
	 */
	public void preHandleEvent(StageEvent event);
	
	/**
	 * Hook when an event has been executed.
	 * This hook is called even if the event handling threw an exception.
	 *  
	 * @param event The event that was executed.
	 * @param exception The exception that occured during event handling,
	 * or null if the event handling succeeded.
	 */
	public void postHandleEvent(StageEvent event, Throwable exception);
}
