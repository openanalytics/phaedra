package eu.openanalytics.phaedra.validation.action.well;

import eu.openanalytics.phaedra.validation.ValidationService.WellStatus;


public class RejectWellAction extends AbstractWellAction {

	@Override
	protected WellStatus getActionStatus() {
		return WellStatus.REJECTED_PHAEDRA;
	}
}
