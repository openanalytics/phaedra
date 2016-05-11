package eu.openanalytics.phaedra.base.http.server;

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
		// Make sure these plugins are active:

		// 1. eu.openanalytics.phaedra.http.server
		Activator.getDefault();

		// 2. org.eclipse.equinox.http.registry (necessary to make sure bundle is activated)
		org.eclipse.equinox.http.registry.HttpContextExtensionService.class.getClass();
	}

}
