package eu.openanalytics.phaedra.model.curve.hook;

import eu.openanalytics.phaedra.base.hook.IHook;
import eu.openanalytics.phaedra.base.hook.IHookArguments;
import eu.openanalytics.phaedra.base.hook.PreHookException;
import eu.openanalytics.phaedra.calculation.hook.CalculationHookArguments;
import eu.openanalytics.phaedra.model.curve.CurveFitService;

/**
 * Triggers curve fitting for plates that were recalculated.
 */
public class PostCalculationFitter implements IHook {

	@Override
	public void pre(IHookArguments args) throws PreHookException {
		// Do nothing.
	}

	@Override
	public void post(IHookArguments args) {
		CalculationHookArguments calculationArgs = (CalculationHookArguments)args;
		CurveFitService.getInstance().fitCurves(calculationArgs.plate);
	}

}
