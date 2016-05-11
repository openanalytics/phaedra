package eu.openanalytics.phaedra.validation.action.plate;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.base.security.model.Permissions;
import eu.openanalytics.phaedra.model.log.ObjectLogService;
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.validation.IValidationAction;
import eu.openanalytics.phaedra.validation.ValidationException;
import eu.openanalytics.phaedra.validation.ValidationService;
import eu.openanalytics.phaedra.validation.ValidationService.PlateApprovalStatus;
import eu.openanalytics.phaedra.validation.ValidationService.PlateStatus;
import eu.openanalytics.phaedra.validation.ValidationService.PlateValidationStatus;


public abstract class AbstractPlateAction implements IValidationAction {

	@Override
	public void run(String remark, Object... objects) throws ValidationException {
		if (objects == null || objects.length == 0) return;
		
		List<Plate> affectedPlates = new ArrayList<>();
		for (Object object: objects) affectedPlates.add((Plate)object);
		
		PlateValidationStatus val = getActionValidationStatus();
		PlateApprovalStatus app = getActionApprovalStatus();
		boolean isValidationAction = (val != null);
		
		// Check permission on all affected plates.
		for (Plate plate: affectedPlates) {
			PlateStatus status = ValidationService.getInstance().getPlateStatus(plate);
			
			if (isValidationAction) { 
				if (status.approvalSet) throw new ValidationException("Cannot change plate validation: plate is already (dis)approved.");
				SecurityService.getInstance().checkWithException(Permissions.PLATE_CHANGE_VALIDATION, plate);
			} else {
				if (!status.validationSet) throw new ValidationException("Cannot change plate approval: plate is not yet validated.");
				SecurityService.getInstance().checkWithException(Permissions.PLATE_CHANGE_APPROVAL, plate);
			}
		}
		
		// Perform the validation/approval operation.
		for (Plate plate: affectedPlates) {
			if (isValidationAction) {
				int oldStatus = plate.getValidationStatus();
				applyValidationStatus(plate, val, remark);
				PlateService.getInstance().updatePlateValidation(plate);
				logValidationChange(plate, remark, oldStatus);
			} else {
				int oldStatus = plate.getApprovalStatus();
				applyApprovalStatus(plate, app, remark);
				PlateService.getInstance().updatePlateValidation(plate);
				logApprovalChange(plate, remark, oldStatus);
			}
		}
	}
	
	protected PlateValidationStatus getActionValidationStatus() {
		return null;
	}
	
	protected PlateApprovalStatus getActionApprovalStatus() {
		return null;
	}
	
	protected void applyValidationStatus(Plate plate, PlateValidationStatus newStatus, String remark) {
		String userName = SecurityService.getInstance().getCurrentUserName();
		Date changeDate = new Date();
		plate.setValidationStatus(newStatus.getCode());
		plate.setValidationDate(changeDate);
		plate.setValidationUser(userName);
		if (remark != null && !remark.isEmpty()) plate.setDescription(remark);
	}
	
	protected void applyApprovalStatus(Plate plate, PlateApprovalStatus newStatus, String remark) {
		String userName = SecurityService.getInstance().getCurrentUserName();
		Date changeDate = new Date();
		PlateApprovalStatus statusToSet = newStatus;
		if (newStatus == PlateApprovalStatus.APPROVED && plate.getValidationStatus() == PlateValidationStatus.INVALIDATED.getCode()) {
			// Special case: invalidated plates get disapproved instead of approved.
			statusToSet = PlateApprovalStatus.DISAPPROVED;
		}
		plate.setApprovalStatus(statusToSet.getCode());
		plate.setApprovalDate(changeDate);
		plate.setApprovalUser(userName);
		if (remark != null && !remark.isEmpty()) plate.setDescription(remark);
	}
	
	protected void logValidationChange(Plate plate, String remark, int oldStatus) {
		if (remark == null || remark.isEmpty()) remark = PlateValidationStatus.getByCode(plate.getValidationStatus()).toString();
		ObjectLogService.getInstance().logChange(plate, "Validation", ""+oldStatus, ""+plate.getValidationStatus(), remark);
	}
	
	protected void logApprovalChange(Plate plate, String remark, int oldStatus) {
		if (remark == null || remark.isEmpty()) remark = PlateApprovalStatus.getByCode(plate.getApprovalStatus()).toString();
		ObjectLogService.getInstance().logChange(plate, "Approval", ""+oldStatus, ""+plate.getApprovalStatus(), remark);
	}
}
