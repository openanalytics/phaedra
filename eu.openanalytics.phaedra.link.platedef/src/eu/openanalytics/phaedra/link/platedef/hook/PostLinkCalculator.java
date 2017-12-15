package eu.openanalytics.phaedra.link.platedef.hook;

import java.util.HashSet;
import java.util.Set;

import eu.openanalytics.phaedra.base.hook.BaseBatchedHook;
import eu.openanalytics.phaedra.base.hook.IHookArguments;
import eu.openanalytics.phaedra.calculation.CalculationService;
import eu.openanalytics.phaedra.calculation.hooks.CalculationHookUtils;
import eu.openanalytics.phaedra.link.platedef.hook.LinkPlateDefHookArguments;
import eu.openanalytics.phaedra.model.curve.CurveFitService;
import eu.openanalytics.phaedra.model.plate.vo.Plate;

public class PostLinkCalculator extends BaseBatchedHook {

	private Set<Plate> platesToRecalc = new HashSet<>();
	private Set<Plate> platesToRefit = new HashSet<>();

	@Override
	protected void processPre(IHookArguments args) {
		if (!needsRecalc(args)) return;
		Plate plate = ((LinkPlateDefHookArguments)args).settings.getPlate();
		// If a plate is multiplo-linked to other plates, and that link is broken by the layout change, refit those plates.
		if (CalculationHookUtils.hasLinkedCurveFitting(plate)) {
			platesToRefit.addAll(CalculationHookUtils.getCurveFittingLinkedPlates(plate));
		}
	}

	@Override
	protected void processPost(IHookArguments args) {
		if (!needsRecalc(args)) return;
		Plate plate = ((LinkPlateDefHookArguments)args).settings.getPlate();
		platesToRecalc.addAll(CalculationHookUtils.getNormalizationLinkedPlates(plate));
	}

	@Override
	protected void processBatch() {
		for (Plate plate: platesToRecalc) CalculationService.getInstance().calculate(plate);
		for (Plate plate: platesToRefit) {
			if (platesToRecalc.contains(plate)) continue;
			CurveFitService.getInstance().fitCurves(plate);
		}
	}
	
	@Override
	protected void reset() {
		platesToRecalc.clear();
		platesToRefit.clear();
	}
	
	private boolean needsRecalc(IHookArguments args) {
		Boolean recalcNeeded = (Boolean) ((LinkPlateDefHookArguments)args).settings.getSettings().get("recalcPlates");
		return (recalcNeeded == null || recalcNeeded.booleanValue());
	}
}
