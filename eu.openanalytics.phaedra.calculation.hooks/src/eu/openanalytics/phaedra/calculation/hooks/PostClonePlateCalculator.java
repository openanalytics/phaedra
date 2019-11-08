package eu.openanalytics.phaedra.calculation.hooks;

import eu.openanalytics.phaedra.base.event.ModelEventType;
import eu.openanalytics.phaedra.base.hook.IHook;
import eu.openanalytics.phaedra.base.hook.IHookArguments;
import eu.openanalytics.phaedra.base.hook.PreHookException;
import eu.openanalytics.phaedra.calculation.CalculationService;
import eu.openanalytics.phaedra.model.plate.hook.PlateActionHookArguments;
import eu.openanalytics.phaedra.model.plate.vo.Plate;

public class PostClonePlateCalculator implements IHook {

	@Override
	public void pre(IHookArguments args) throws PreHookException {
		// Do nothing.
	}

	@Override
	public void post(IHookArguments args) {
		PlateActionHookArguments linkArgs = (PlateActionHookArguments)args;
		if (linkArgs.action != ModelEventType.ObjectCloned) return;
		
		for (Plate plate: CalculationHookUtils.getNormalizationLinkedPlates(linkArgs.plate)) {
			CalculationService.getInstance().getAccessor(plate).reset();
			CalculationService.getInstance().calculate(plate);
		}
	}

}
