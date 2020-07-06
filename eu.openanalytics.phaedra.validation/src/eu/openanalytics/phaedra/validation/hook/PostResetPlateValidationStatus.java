package eu.openanalytics.phaedra.validation.hook;

import java.util.ArrayList;
import java.util.List;

import eu.openanalytics.phaedra.base.hook.IHook;
import eu.openanalytics.phaedra.base.hook.IHookArguments;
import eu.openanalytics.phaedra.base.hook.PreHookException;
import eu.openanalytics.phaedra.model.plate.vo.Compound;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.validation.ValidationService;
import eu.openanalytics.phaedra.validation.ValidationService.Action;
import eu.openanalytics.phaedra.validation.ValidationService.CompoundValidationStatus;

public class PostResetPlateValidationStatus implements IHook {

	@Override
	public void pre(IHookArguments args) throws PreHookException {
		// Do nothing.
	}

	@Override
	public void post(IHookArguments args) {
		ValidationHookArguments validationArgs = (ValidationHookArguments)args;
		if (validationArgs.action != Action.RESET_PLATE_VALIDATION) return;
		
		List<Plate> plates = new ArrayList<>();
		for (Object o: validationArgs.objects) if (o instanceof Plate) plates.add((Plate)o);
		if (plates.isEmpty()) return;
		
		List<Compound> compoundsToReset = new ArrayList<>();
		for (Plate plate: plates) {
			if (plate.getValidationStatus() > 0) continue;
			for (Compound c: plate.getCompounds()) {
				if (c.getValidationStatus() == CompoundValidationStatus.VALIDATION_NOT_SET.getCode()) continue;
				compoundsToReset.add(c);
			}
		}
		if (compoundsToReset.isEmpty()) return;
		
		ValidationService.getInstance().doAction(Action.RESET_COMPOUND_VALIDATION, "Auto reset compound validation status", false, compoundsToReset.toArray());
	}

}
