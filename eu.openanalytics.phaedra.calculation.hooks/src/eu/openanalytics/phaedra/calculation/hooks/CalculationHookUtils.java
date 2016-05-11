package eu.openanalytics.phaedra.calculation.hooks;

import java.util.ArrayList;
import java.util.List;

import eu.openanalytics.phaedra.calculation.CalculationService;
import eu.openanalytics.phaedra.calculation.norm.NormalizationService.NormalizationScope;
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.validation.ValidationException;
import eu.openanalytics.phaedra.validation.ValidationService;
import eu.openanalytics.phaedra.validation.ValidationService.PlateApprovalStatus;
import eu.openanalytics.phaedra.validation.ValidationService.PlateStatus;
import eu.openanalytics.phaedra.validation.ValidationService.PlateValidationStatus;

public class CalculationHookUtils {

	public static boolean hasLinkedNormalization(Plate plate) {
		for (Feature feature: PlateUtils.getFeatures(plate)) {
			if (feature.getNormalizationScope() == NormalizationScope.ExperimentWide.getId()) {
				return true;
			}
		}
		return false;
	}
	
	public static boolean hasLinkedCurveFitting(Plate plate) {
		return CalculationService.getInstance().isMultiplo(plate.getExperiment());
	}
	
	public static List<Plate> getNormalizationLinkedPlates(Plate plate) {
		List<Plate> plates = new ArrayList<>();
		if (hasLinkedNormalization(plate)) {
			plates.addAll(PlateService.getInstance().getPlates(plate.getExperiment()));
		} else {
			plates.add(plate);
		}
		return plates;
	}
	
	public static List<Plate> getCurveFittingLinkedPlates(Plate plate) {
		List<Plate> plates = CalculationService.getInstance().getMultiploPlates(plate);
		return plates;
	}
	
	public static void isRecalculateAllowed(Plate plate) throws ValidationException {
		PlateStatus status = ValidationService.getInstance().getPlateStatus(plate);
		if (status.validationStatus == PlateValidationStatus.VALIDATED) throw new ValidationException("Plate is validated");
		
		if (hasLinkedNormalization(plate)) {
			// Normalization-linked plates will be recalculated as well, so may not be VALIDATED
			for (Plate p: getNormalizationLinkedPlates(plate)) {
				status = ValidationService.getInstance().getPlateStatus(p);
				if (status.validationStatus == PlateValidationStatus.VALIDATED) throw new ValidationException("A normalization-linked plate is validated");
			}
		}
		if (hasLinkedCurveFitting(plate)) {
			// Multiplo-linked plates will be refit, so may not be APPROVED
			for (Plate p: getCurveFittingLinkedPlates(plate)) {
				status = ValidationService.getInstance().getPlateStatus(p);
				if (status.approvalStatus == PlateApprovalStatus.APPROVED) throw new ValidationException("A multiplo-linked plate is approved");
			}
		}
	}
	
	public static void isRefitAllowed(Plate plate) throws ValidationException {
		PlateStatus status = ValidationService.getInstance().getPlateStatus(plate);
		if (status.approvalStatus == PlateApprovalStatus.APPROVED) throw new ValidationException("Plate is approved");

		if (hasLinkedCurveFitting(plate)) {
			// Multiplo-linked plates will be refit as well, so may not be APPROVED
			for (Plate p: getCurveFittingLinkedPlates(plate)) {
				status = ValidationService.getInstance().getPlateStatus(p);
				if (status.approvalStatus == PlateApprovalStatus.APPROVED) throw new ValidationException("A multiplo-linked plate is approved");
			}
		}
	}
	
}
