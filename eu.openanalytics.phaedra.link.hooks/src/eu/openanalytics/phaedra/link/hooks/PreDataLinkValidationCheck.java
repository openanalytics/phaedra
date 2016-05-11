package eu.openanalytics.phaedra.link.hooks;

import eu.openanalytics.phaedra.base.hook.IHook;
import eu.openanalytics.phaedra.base.hook.IHookArguments;
import eu.openanalytics.phaedra.base.hook.PreHookException;
import eu.openanalytics.phaedra.calculation.hooks.CalculationHookUtils;
import eu.openanalytics.phaedra.link.data.hook.LinkDataHookArguments;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.validation.ValidationException;

public class PreDataLinkValidationCheck implements IHook {

	@Override
	public void pre(IHookArguments args) throws PreHookException {
		Plate plate = ((LinkDataHookArguments)args).plate;
		if (plate == null) return;
		try {
			CalculationHookUtils.isRecalculateAllowed(plate);
		} catch (ValidationException e) {
			throw new PreHookException("Data linking is not allowed for " + plate, e);
		}
	}

	@Override
	public void post(IHookArguments args) {
		// Do nothing.
	}

}
