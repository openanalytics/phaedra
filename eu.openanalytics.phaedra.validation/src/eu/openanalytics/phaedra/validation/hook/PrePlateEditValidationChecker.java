package eu.openanalytics.phaedra.validation.hook;

import eu.openanalytics.phaedra.base.event.ModelEventType;
import eu.openanalytics.phaedra.base.hook.IHook;
import eu.openanalytics.phaedra.base.hook.IHookArguments;
import eu.openanalytics.phaedra.base.hook.PreHookException;
import eu.openanalytics.phaedra.model.plate.hook.PlateActionHookArguments;
import eu.openanalytics.phaedra.validation.ValidationService;
import eu.openanalytics.phaedra.validation.ValidationService.PlateApprovalStatus;
import eu.openanalytics.phaedra.validation.ValidationService.PlateStatus;

public class PrePlateEditValidationChecker implements IHook {

	@Override
	public void pre(IHookArguments args) throws PreHookException {
		// Allow the plate modification only if the approval is not set.
		PlateActionHookArguments plateArgs = (PlateActionHookArguments)args;
		if (plateArgs.action == ModelEventType.ObjectChanged) {
			PlateStatus plateStatus = ValidationService.getInstance().getPlateStatus(plateArgs.plate);
			if (plateStatus.approvalStatus == PlateApprovalStatus.APPROVED) throw new PreHookException("Cannot modify plate: plate is already approved.");
		}
	}

	@Override
	public void post(IHookArguments args) {
		// Do nothing.
	}

}
