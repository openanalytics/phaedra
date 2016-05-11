package eu.openanalytics.phaedra.base.hook;

/**
 * <p>
 * Interface for a hook that supports batch processing.
 *</p><p>
 * If the hook point supports batch processing (not all of them do),
 * it will mark the start and end of a batch by calling the methods of this interface.
 * </p><p>
 * Between the start and end of a batch, the pre and post hooks are called as normal.
 * </p><p>
 * Good implementations of this interface should be able to handle the hook without batch mode
 * as well (i.e. assume that startBatch and endBatch may never be called at all).
 * </p>
 */
public interface IBatchedHook extends IHook {

	/**
	 * Marks the start of a batch. If the hook point does not support batch mode,
	 * this method will never be called.
	 * 
	 * @param args The arguments describing the hook point.
	 */
	public void startBatch(IBatchedHookArguments args);
	
	/**
	 * Marks the end of a batch. If the hook point does not support batch mode,
	 * this method will never be called.
	 * 
	 * @param successful True if the batch was completed successfully. False if an exception occurred.
	 */
	public void endBatch(boolean successful);
}
