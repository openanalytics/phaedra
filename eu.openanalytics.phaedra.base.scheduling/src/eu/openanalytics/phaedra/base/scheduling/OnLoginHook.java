package eu.openanalytics.phaedra.base.scheduling;

import eu.openanalytics.phaedra.base.hook.IHook;
import eu.openanalytics.phaedra.base.hook.IHookArguments;
import eu.openanalytics.phaedra.base.hook.PreHookException;

public class OnLoginHook implements IHook {

	@Override
	public void pre(IHookArguments args) throws PreHookException {
		// Do nothing.
	}

	@Override
	public void post(IHookArguments args) {
		// Do nothing. The plugin has been activated, that is enough to initialize the scheduler.		
	}

}
