package eu.openanalytics.phaedra.calculation.hooks;

import eu.openanalytics.phaedra.base.hook.IHook;
import eu.openanalytics.phaedra.base.hook.IHookArguments;
import eu.openanalytics.phaedra.base.hook.PreHookException;
import eu.openanalytics.phaedra.calculation.hook.CalculationHookArguments;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.validation.ValidationException;

public class PreCalculationValidationCheck implements IHook {

	@Override
	public void pre(IHookArguments args) throws PreHookException {
		Plate plate = ((CalculationHookArguments)args).plate;
		try {
			CalculationHookUtils.isRecalculateAllowed(plate);
		} catch (ValidationException e) {
			throw new PreHookException("Recalculation is not allowed for " + plate, e);
		}
	}

	@Override
	public void post(IHookArguments args) {
		// Do nothing.
	}

}
