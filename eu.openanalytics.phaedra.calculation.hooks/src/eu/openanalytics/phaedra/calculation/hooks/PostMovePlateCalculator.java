package eu.openanalytics.phaedra.calculation.hooks;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import eu.openanalytics.phaedra.base.event.ModelEventType;
import eu.openanalytics.phaedra.base.hook.BaseBatchedHook;
import eu.openanalytics.phaedra.base.hook.IHookArguments;
import eu.openanalytics.phaedra.calculation.CalculationService;
import eu.openanalytics.phaedra.model.curve.CurveService;
import eu.openanalytics.phaedra.model.plate.hook.PlateActionHookArguments;
import eu.openanalytics.phaedra.model.plate.vo.Plate;

public class PostMovePlateCalculator extends BaseBatchedHook {

	private Set<Plate> platesToRecalc = new HashSet<>();
	private Set<Plate> platesToRefit = new HashSet<>();
	
	@Override
	protected boolean shouldProcess(IHookArguments args) {
		return (((PlateActionHookArguments)args).action == ModelEventType.ObjectMoved);
	}
	
	@Override
	protected void processPre(IHookArguments args) {
		Plate plate = ((PlateActionHookArguments)args).plate;
		
		// Plates that shared normalization before the move, must be recalculated.
		if (CalculationHookUtils.hasLinkedNormalization(plate)) {
			List<Plate> plates = CalculationHookUtils.getNormalizationLinkedPlates(plate);
			if (plates.size() > 1) platesToRecalc.addAll(plates);
		}
		
		// Plates that shared multiplo curves before the move, must be refit.
		if (CalculationHookUtils.hasLinkedCurveFitting(plate)) {
			List<Plate> plates = CalculationHookUtils.getCurveFittingLinkedPlates(plate);
			if (plates.size() > 1) platesToRefit.addAll(plates);
		}
	}

	@Override
	protected void processPost(IHookArguments args) {
		Plate plate = ((PlateActionHookArguments)args).plate;

		// Plates that share normalization after the move, must be recalculated.
		// This includes the plate itself, even if it is the only plate in the destination experiment.
		if (CalculationHookUtils.hasLinkedNormalization(plate)) {
			platesToRecalc.addAll(CalculationHookUtils.getNormalizationLinkedPlates(plate));
		}
		
		// Refit the moved plate, as appropriate.
		if (CalculationHookUtils.hasLinkedCurveFitting(plate)) platesToRefit.add(plate);
	}
	
	@Override
	protected void processBatch() {
		for (Plate plate: platesToRecalc) CalculationService.getInstance().calculate(plate);
		for (Plate plate: platesToRefit) {
			if (platesToRecalc.contains(plate)) continue;
			CurveService.getInstance().fitAllCurves(plate);
		}
	}
	
	@Override
	protected void reset() {
		platesToRecalc.clear();
		platesToRefit.clear();
	}
}
