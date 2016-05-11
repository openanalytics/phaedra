package eu.openanalytics.phaedra.validation.action.compound;

import eu.openanalytics.phaedra.validation.ValidationService.CompoundValidationStatus;


public class ResetCompoundValidationAction extends AbstractCompoundAction {

	@Override
	protected CompoundValidationStatus getActionStatus() {
		return CompoundValidationStatus.VALIDATION_NOT_SET;
	}
}