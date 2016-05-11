package eu.openanalytics.phaedra.calculation.hooks;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import eu.openanalytics.phaedra.base.hook.BaseBatchedHook;
import eu.openanalytics.phaedra.base.hook.IHookArguments;
import eu.openanalytics.phaedra.calculation.CalculationService;
import eu.openanalytics.phaedra.model.curve.CurveService;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Compound;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.validation.ValidationService;
import eu.openanalytics.phaedra.validation.ValidationService.CompoundValidationStatus;
import eu.openanalytics.phaedra.validation.ValidationService.PlateValidationStatus;
import eu.openanalytics.phaedra.validation.hook.ValidationHookArguments;

public class PostValidationCalculator extends BaseBatchedHook {

	private Set<Plate> platesToRecalc = new HashSet<>();
	private Set<Plate> platesToRefit = new HashSet<>();
	private Map<Plate, PlateValidationStatus> platePreChangeStatus = new HashMap<>();
	private Map<Compound, CompoundValidationStatus> compoundPreChangeStatus = new HashMap<>();
	
	@Override
	protected void processPre(IHookArguments args) {
		// Remember the validation status before the change so it can be compared against later.
		ValidationHookArguments validationArgs = (ValidationHookArguments)args;
		for (Object item: validationArgs.objects) {
			if (ValidationService.isValidationAction(validationArgs.action)) {
				if (item instanceof Plate) platePreChangeStatus.put((Plate) item, PlateValidationStatus.getByCode(((Plate) item).getValidationStatus()));
				if (item instanceof Compound) compoundPreChangeStatus.put((Compound) item, CompoundValidationStatus.getByCode(((Compound) item).getValidationStatus()));
			}
		}
	}

	@Override
	protected void processPost(IHookArguments args) {
		ValidationHookArguments validationArgs = (ValidationHookArguments)args;
		for (Object item: validationArgs.objects) {
			if (ValidationService.isWellAction(validationArgs.action) && item instanceof Well && PlateUtils.isControl((Well) item)) {
				// A control well was rejected/accepted
				Well well = (Well) item;
				platesToRecalc.addAll(CalculationHookUtils.getNormalizationLinkedPlates(well.getPlate()));
			} else if (ValidationService.isValidationAction(validationArgs.action) && item instanceof Plate) {
				Plate plate = (Plate) item;
				// If a plate is not linked to other plates, validation/approval actions do not trigger recalculation.
				if (!CalculationHookUtils.hasLinkedNormalization(plate) && !CalculationHookUtils.hasLinkedCurveFitting(plate)) continue;
				
				PlateValidationStatus oldStatus = platePreChangeStatus.get(plate);
				PlateValidationStatus newStatus = PlateValidationStatus.getByCode(plate.getValidationStatus());
				boolean validationChanges = false;
				if (oldStatus != PlateValidationStatus.INVALIDATED && newStatus == PlateValidationStatus.INVALIDATED) validationChanges = true;
				if (oldStatus == PlateValidationStatus.INVALIDATED && newStatus != PlateValidationStatus.INVALIDATED) validationChanges = true;
				if (!validationChanges) continue;
				
				if (CalculationHookUtils.hasLinkedNormalization(plate)) {
					platesToRecalc.addAll(CalculationHookUtils.getNormalizationLinkedPlates(plate));
				}
				if (CalculationHookUtils.hasLinkedCurveFitting(plate)) {
					platesToRefit.add(plate);
				}
			} else if (ValidationService.isValidationAction(validationArgs.action) && item instanceof Compound) {
				Compound compound = (Compound) item;
				if (!CalculationHookUtils.hasLinkedCurveFitting(compound.getPlate())) continue;

				CompoundValidationStatus oldStatus = compoundPreChangeStatus.get(compound);
				CompoundValidationStatus newStatus = CompoundValidationStatus.getByCode(compound.getValidationStatus());
				boolean validationChanges = false;
				if (oldStatus != CompoundValidationStatus.INVALIDATED && newStatus == CompoundValidationStatus.INVALIDATED) validationChanges = true;
				if (oldStatus == CompoundValidationStatus.INVALIDATED && newStatus != CompoundValidationStatus.INVALIDATED) validationChanges = true;
				if (!validationChanges) continue;
				
				platesToRefit.add(compound.getPlate());
			}
		}
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
		platePreChangeStatus.clear();
		compoundPreChangeStatus.clear();
	}
}
