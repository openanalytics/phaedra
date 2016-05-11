package eu.openanalytics.phaedra.validation.action.compound;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.base.security.model.Permissions;
import eu.openanalytics.phaedra.model.log.ObjectLogService;
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.vo.Compound;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.validation.IValidationAction;
import eu.openanalytics.phaedra.validation.ValidationException;
import eu.openanalytics.phaedra.validation.ValidationService;
import eu.openanalytics.phaedra.validation.ValidationService.CompoundValidationStatus;
import eu.openanalytics.phaedra.validation.ValidationService.PlateStatus;


public abstract class AbstractCompoundAction implements IValidationAction {

	@Override
	public void run(String remark, Object... objects) throws ValidationException {
		if (objects == null || objects.length == 0) return;
		
		// Order all affected compounds by their plate(s).
		Map<Plate, List<Compound>> affectedPlates = new HashMap<Plate, List<Compound>>();
		for (Object object: objects) {
			Compound compound = (Compound)object;
			List<Compound> affectedCompounds = affectedPlates.get(compound.getPlate());
			if (affectedCompounds == null) {
				affectedCompounds = new ArrayList<>();
				affectedPlates.put(compound.getPlate(), affectedCompounds);
			}
			affectedCompounds.add(compound);
		}
		
		String userName = SecurityService.getInstance().getCurrentUserName();
		Date validationDate = new Date();
		int newStatus = getActionStatus().getCode();
		
		// Check permission and status for all affected compounds.
		for (Plate plate: affectedPlates.keySet()) {
			PlateStatus status = ValidationService.getInstance().getPlateStatus(plate);
			if (status.approvalSet) throw new ValidationException("Cannot change compound status: plate is already (dis)approved.");
			if (status.validationSet && plate.getValidationStatus() < 0 && newStatus >= 0) throw new ValidationException("Cannot change compound status: plate is invalidated.");
			
			for (Compound compound: affectedPlates.get(plate)) {
				if (compound.getValidationStatus() == CompoundValidationStatus.VALIDATION_NOT_NEEDED.getCode()) {
					throw new ValidationException("Compound validation not needed: the compound is tested in single-dose.");
				} else {
					SecurityService.getInstance().checkWithException(Permissions.COMPOUND_CHANGE_VALIDATION, compound);
				}
			}
		}
		
		// Perform the validation operation.
		for (Plate plate: affectedPlates.keySet()) {
			for (Compound compound: affectedPlates.get(plate)) {
				int oldStatus = compound.getValidationStatus();
				compound.setValidationStatus(newStatus);
				compound.setValidationDate(validationDate);
				compound.setValidationUser(userName);
				PlateService.getInstance().updateCompound(compound);
				logValidationChange(compound, remark, oldStatus);
			}
			PlateService.getInstance().updatePlateValidation(plate);
		}
	}
	
	protected abstract CompoundValidationStatus getActionStatus();
	
	protected void logValidationChange(Compound compound, String remark, int oldStatus) {
		if (remark == null || remark.isEmpty()) remark = CompoundValidationStatus.getByCode(compound.getValidationStatus()).toString();
		ObjectLogService.getInstance().logChange(compound, "Validation", ""+oldStatus, ""+compound.getValidationStatus(), remark);
	}
}
