package eu.openanalytics.phaedra.validation.action.plate;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.base.security.model.Permissions;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.model.log.ObjectLogService;
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.validation.Activator;
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
		appendRemarkToDescription(plate, remark, INVALIDATION_REMARK_PATTERN, "Invalidated");
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
		appendRemarkToDescription(plate, remark, DISAPPROVAL_REMARK_PATTERN, "Disapproved");
	}
	
	private static final Pattern INVALIDATION_REMARK_PATTERN = Pattern.compile("(.*)Invalidated:.*;(.*)");
	private static final Pattern DISAPPROVAL_REMARK_PATTERN = Pattern.compile("(.*)Disapproved:.*;(.*)");
	
	protected void appendRemarkToDescription(Plate plate, String remark, Pattern pattern, String keyword) {
		String currentDescription = plate.getDescription();
		String newDescription = currentDescription;
		if (remark == null || remark.trim().isEmpty()) {
			// Remove any invalidation/disapproval remark from the description
			if (currentDescription == null || currentDescription.trim().isEmpty()) return;
			else {
				Matcher matcher = pattern.matcher(currentDescription);
				if (matcher.matches()) newDescription = (matcher.group(1) + " " + matcher.group(2)).trim();
			}
		} else {
			// Add or replace the invalidation/disapproval remark in the description
			if (currentDescription == null) currentDescription = "";
			String append = " " + keyword + ": " + remark + ";";
			Matcher matcher = pattern.matcher(currentDescription);
			if (matcher.matches()) newDescription = (matcher.group(1) + append + matcher.group(2)).trim();
			else newDescription = (currentDescription + append).trim();
		}
		EclipseLog.info(String.format("Changed description from '%s' to '%s'", currentDescription, newDescription), Activator.PLUGIN_ID);
		plate.setDescription(newDescription);
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
