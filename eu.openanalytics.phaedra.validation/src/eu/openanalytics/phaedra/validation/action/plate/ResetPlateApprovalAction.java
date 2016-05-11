package eu.openanalytics.phaedra.validation.action.plate;

import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.validation.ValidationService.PlateApprovalStatus;


public class ResetPlateApprovalAction extends AbstractPlateAction {

	@Override
	protected PlateApprovalStatus getActionApprovalStatus() {
		return PlateApprovalStatus.APPROVAL_NOT_SET;
	}
	
	@Override
	protected void applyApprovalStatus(Plate plate, PlateApprovalStatus newStatus, String remark) {
		super.applyApprovalStatus(plate, newStatus, remark);
		plate.setApprovalDate(null);
		plate.setApprovalUser(null);
	}
}