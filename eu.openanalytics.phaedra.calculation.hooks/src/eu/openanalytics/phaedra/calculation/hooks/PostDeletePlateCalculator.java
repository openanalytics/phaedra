package eu.openanalytics.phaedra.calculation.hooks;

import java.util.HashSet;
import java.util.Set;

import eu.openanalytics.phaedra.base.event.ModelEventType;
import eu.openanalytics.phaedra.base.hook.BaseBatchedHook;
import eu.openanalytics.phaedra.base.hook.IHookArguments;
import eu.openanalytics.phaedra.calculation.CalculationService;
import eu.openanalytics.phaedra.model.curve.CurveFitService;
import eu.openanalytics.phaedra.model.plate.hook.PlateActionHookArguments;
import eu.openanalytics.phaedra.model.plate.vo.Plate;

public class PostDeletePlateCalculator extends BaseBatchedHook {

	private Set<Plate> platesToRecalc = new HashSet<>();
	private Set<Plate> platesToRefit = new HashSet<>();
	
	@Override
	protected boolean shouldProcess(IHookArguments args) {
		return (((PlateActionHookArguments)args).action == ModelEventType.ObjectRemoved);
	}
	
	@Override
	protected void processPre(IHookArguments args) {
		Plate plate = ((PlateActionHookArguments)args).plate;
		
		// Plates that shared normalization before the delete, must be recalculated.
		if (CalculationHookUtils.hasLinkedNormalization(plate)) {
			for (Plate p: CalculationHookUtils.getNormalizationLinkedPlates(plate)) {
				if (p != plate) platesToRecalc.add(p);
			}
		}
		
		// Plates that shared multiplo curves before the delete, must be refit.
		if (CalculationHookUtils.hasLinkedCurveFitting(plate)) {
			for (Plate p: CalculationHookUtils.getCurveFittingLinkedPlates(plate)) {
				if (p != plate) platesToRefit.add(p);
			}
		}
	}

	@Override
	protected void processPost(IHookArguments args) {
		// Nothing to do.
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
}
