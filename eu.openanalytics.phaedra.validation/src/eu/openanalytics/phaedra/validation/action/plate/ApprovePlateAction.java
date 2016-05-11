package eu.openanalytics.phaedra.validation.action.plate;

import eu.openanalytics.phaedra.validation.ValidationService.PlateApprovalStatus;

public class ApprovePlateAction extends AbstractPlateAction {

	@Override
	protected PlateApprovalStatus getActionApprovalStatus() {
		return PlateApprovalStatus.APPROVED;
	}
}