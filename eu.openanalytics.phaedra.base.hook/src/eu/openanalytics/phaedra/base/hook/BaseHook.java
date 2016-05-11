package eu.openanalytics.phaedra.base.hook;

public class BaseHook implements IHook {

	@Override
	public void pre(IHookArguments args) throws PreHookException {
		// Default: do nothing.
	}

	@Override
	public void post(IHookArguments args) {
		// Default: do nothing.
	}

}
