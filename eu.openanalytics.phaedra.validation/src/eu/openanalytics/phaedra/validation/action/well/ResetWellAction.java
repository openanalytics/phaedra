package eu.openanalytics.phaedra.validation.action.well;

import eu.openanalytics.phaedra.validation.ValidationService.WellStatus;


public class ResetWellAction extends AbstractWellAction {

	@Override
	protected WellStatus getActionStatus() {
		return WellStatus.ACCEPTED_DEFAULT;
	}
}