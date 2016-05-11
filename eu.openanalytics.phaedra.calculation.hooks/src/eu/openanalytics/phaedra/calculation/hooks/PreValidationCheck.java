package eu.openanalytics.phaedra.calculation.hooks;

import java.util.HashSet;
import java.util.Set;

import eu.openanalytics.phaedra.base.hook.IHook;
import eu.openanalytics.phaedra.base.hook.IHookArguments;
import eu.openanalytics.phaedra.base.hook.PreHookException;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.validation.ValidationException;
import eu.openanalytics.phaedra.validation.ValidationService;
import eu.openanalytics.phaedra.validation.ValidationService.Action;
import eu.openanalytics.phaedra.validation.ValidationService.PlateStatus;
import eu.openanalytics.phaedra.validation.ValidationService.PlateValidationStatus;
import eu.openanalytics.phaedra.validation.hook.ValidationHookArguments;

/**
 * Check the status of normalization- or multiplo-linked plates.
 * Note: base validation checks (e.g. reject well in approved plate) are done by the validation action itself.
 */
public class PreValidationCheck implements IHook {

	@Override
	public void pre(IHookArguments args) throws PreHookException {
		ValidationHookArguments validationArgs = (ValidationHookArguments)args;
		Action action = validationArgs.action;

		// Sample well: triggers refit
		// Control well: triggers recalc
		// Val plate: may trigger recalc or refit
		
		try {
			Set<Plate> platesToRecalc = new HashSet<>();
			Set<Plate> platesToRefit = new HashSet<>();
			
			for (Object object: validationArgs.objects) {
				Plate plate = SelectionUtils.getAsClass(object, Plate.class);
				Well well = SelectionUtils.getAsClass(object, Well.class);

				if (ValidationService.isWellAction(action) && well != null) {
					if (PlateUtils.isControl(well)) platesToRecalc.add(plate);
					else platesToRefit.add(plate);
				} else if (ValidationService.isValidationAction(action)) {
					// If a plate is not linked to other plates, validation/approval actions do not trigger recalculation.
					if (!CalculationHookUtils.hasLinkedNormalization(plate) && !CalculationHookUtils.hasLinkedCurveFitting(plate)) continue;
					
					boolean validationChanges = false;
					PlateStatus status = ValidationService.getInstance().getPlateStatus(plate);
					if (status.validationStatus.getCode() >= 0 && action == Action.INVALIDATE_PLATE) validationChanges = true;
					if (status.validationStatus.getCode() < 0 && action != Action.INVALIDATE_PLATE) validationChanges = true;
					
					if (validationChanges && CalculationHookUtils.hasLinkedNormalization(plate)) platesToRecalc.add(plate);
					else if (validationChanges && CalculationHookUtils.hasLinkedCurveFitting(plate)) platesToRefit.add(plate);
				} else if (ValidationService.isApprovalAction(action)) {
					if (!CalculationHookUtils.hasLinkedNormalization(plate) && !CalculationHookUtils.hasLinkedCurveFitting(plate)) continue;
					
					PlateStatus status = ValidationService.getInstance().getPlateStatus(plate);
					boolean validated = status.validationStatus == PlateValidationStatus.VALIDATED;
					if (validated && action == Action.DISAPPROVE_PLATE) throw new ValidationException(
							"Cannot disapprove: this is a multiplo plate. Please invalidate all affected multiplo plates first.");
				}
			}
			
			for (Plate plate: platesToRecalc) CalculationHookUtils.isRecalculateAllowed(plate);
			for (Plate plate: platesToRefit) CalculationHookUtils.isRefitAllowed(plate);
			
		} catch (ValidationException e) {
			throw new PreHookException("Action not allowed", e);
		}
	}

	@Override
	public void post(IHookArguments args) {
		// Do nothing.
	}
}
