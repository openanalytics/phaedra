package eu.openanalytics.phaedra.base.hook;

/**
 * Base implementation for a batched hook that treats non-batched operations
 * as single-item batches.
 */
public abstract class BaseBatchedHook implements IBatchedHook {

	private boolean batchMode;
	
	@Override
	public void startBatch(IBatchedHookArguments args) {
		reset();
		batchMode = true;
	}

	@Override
	public void endBatch(boolean successful) {
		if (successful) processBatch();
		reset();
		batchMode = false;
	}
	
	@Override
	public void pre(IHookArguments args) throws PreHookException {
		if (!shouldProcess(args)) return;
		if (!batchMode) reset();
		processPre(args);
	}

	@Override
	public void post(IHookArguments args) {
		if (!shouldProcess(args)) return;
		processPost(args);
		if (!batchMode) {
			processBatch();
			reset();
		}
	}

	protected abstract void reset();
	
	protected abstract void processBatch();
	
	protected boolean isBatchMode() {
		return batchMode;
	}
	
	protected boolean shouldProcess(IHookArguments args) {
		// Default: always process.
		return true;
	}
	
	protected void processPre(IHookArguments args) {
		// Default: do nothing.
	}
	
	protected void processPost(IHookArguments args) {
		// Default: do nothing.
	}
}
