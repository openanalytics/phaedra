package eu.openanalytics.phaedra.validation.hook;

import eu.openanalytics.phaedra.base.hook.HookService;
import eu.openanalytics.phaedra.base.hook.IHookArguments;
import eu.openanalytics.phaedra.base.hook.PreHookException;
import eu.openanalytics.phaedra.validation.Activator;
import eu.openanalytics.phaedra.validation.ValidationException;
import eu.openanalytics.phaedra.validation.ValidationService.Action;

public class ValidationHookManager {

	public final static String HOOK_POINT_ID = Activator.PLUGIN_ID + ".validationHookPoint";

	public void preValidation(Action action, Object[] objects) throws ValidationException {
		IHookArguments args = new ValidationHookArguments(action, objects);
		try {
			HookService.getInstance().runPre(HOOK_POINT_ID, args);
		} catch (PreHookException e) {
			throw new ValidationException(e.getMessage(), e.getCause());
		}
	}
	
	public void postValidation(Action action, Object[] objects) {
		IHookArguments args = new ValidationHookArguments(action, objects);
		HookService.getInstance().runPost(HOOK_POINT_ID, args);
	}
}
