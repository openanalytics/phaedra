package eu.openanalytics.phaedra.validation.action.plate;

import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.validation.ValidationService.PlateValidationStatus;


public class ResetPlateValidationAction extends AbstractPlateAction {

	@Override
	protected PlateValidationStatus getActionValidationStatus() {
		return PlateValidationStatus.VALIDATION_NOT_SET;
	}
	
	@Override
	protected void applyValidationStatus(Plate plate, PlateValidationStatus newStatus, String remark) {
		super.applyValidationStatus(plate, newStatus, remark);
		plate.setValidationDate(null);
		plate.setValidationUser(null);
	}
}