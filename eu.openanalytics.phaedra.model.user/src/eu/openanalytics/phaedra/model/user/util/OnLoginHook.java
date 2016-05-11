package eu.openanalytics.phaedra.model.user.util;

import eu.openanalytics.phaedra.base.hook.IHook;
import eu.openanalytics.phaedra.base.hook.IHookArguments;
import eu.openanalytics.phaedra.base.hook.PreHookException;
import eu.openanalytics.phaedra.base.pref.store.GlobalPrefenceAccessor;
import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.base.util.misc.VersionUtils;
import eu.openanalytics.phaedra.model.user.UserService;

public class OnLoginHook implements IHook {

	@Override
	public void pre(IHookArguments args) throws PreHookException {
		// Do nothing
	}

	@Override
	public void post(IHookArguments args) {
		String userName = SecurityService.getInstance().getCurrentUserName();
		UserService.getInstance().logSession(userName, null, VersionUtils.getPhaedraVersion());
		GlobalPrefenceAccessor.loadPreferences();
	}

}
