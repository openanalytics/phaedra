package eu.openanalytics.phaedra.calculation.hook;

import eu.openanalytics.phaedra.base.hook.HookService;
import eu.openanalytics.phaedra.base.hook.PreHookException;
import eu.openanalytics.phaedra.calculation.Activator;
import eu.openanalytics.phaedra.calculation.CalculationException;
import eu.openanalytics.phaedra.model.plate.vo.Plate;

public class CalculationHookManager {
	
	public final static String HOOK_POINT_ID = Activator.PLUGIN_ID + ".calculationHookPoint";

	public void preCalculation(Plate plate) throws CalculationException {
		CalculationHookArguments args = new CalculationHookArguments(plate);
		try {
			HookService.getInstance().runPre(HOOK_POINT_ID, args);
		} catch (PreHookException e) {
			throw new CalculationException(e.getMessage(), e.getCause());
		}
	}
	
	public void postCalculation(Plate plate) {
		CalculationHookArguments args = new CalculationHookArguments(plate);
		HookService.getInstance().runPost(HOOK_POINT_ID, args);
	}
}
