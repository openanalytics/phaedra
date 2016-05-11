package eu.openanalytics.phaedra.link.hooks;

import eu.openanalytics.phaedra.base.hook.IHook;
import eu.openanalytics.phaedra.base.hook.IHookArguments;
import eu.openanalytics.phaedra.base.hook.PreHookException;
import eu.openanalytics.phaedra.calculation.hooks.CalculationHookUtils;
import eu.openanalytics.phaedra.link.platedef.hook.LinkPlateDefHookArguments;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.validation.ValidationException;

/**
 * Prohibit plate definition linking on a plate that is not recalculatable,
 * e.g. validated, or normalization-linked to a validated plate.
 */
public class PrePlatedefLinkValidationCheck implements IHook {

	@Override
	public void pre(IHookArguments args) throws PreHookException {
		Plate plate = ((LinkPlateDefHookArguments)args).settings.getPlate();
		try {
			CalculationHookUtils.isRecalculateAllowed(plate);
		} catch (ValidationException e) {
			throw new PreHookException("Layout change is not allowed for " + plate, e);
		}
	}

	@Override
	public void post(IHookArguments args) {
		// Do nothing.
	}

}
