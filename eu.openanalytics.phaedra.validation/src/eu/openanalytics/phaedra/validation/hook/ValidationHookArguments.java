package eu.openanalytics.phaedra.validation.hook;

import eu.openanalytics.phaedra.base.hook.IHookArguments;
import eu.openanalytics.phaedra.validation.ValidationService.Action;

public class ValidationHookArguments implements IHookArguments {

	public ValidationHookArguments(Action action, Object[] objects) {
		this.action = action;
		this.objects = objects;
	}
	
	public Action action;
	public Object[] objects;
	
}
