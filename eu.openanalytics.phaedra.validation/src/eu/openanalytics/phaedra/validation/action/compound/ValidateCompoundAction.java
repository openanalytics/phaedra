package eu.openanalytics.phaedra.validation.action.compound;

import eu.openanalytics.phaedra.validation.ValidationService.CompoundValidationStatus;


public class ValidateCompoundAction extends AbstractCompoundAction {

	@Override
	protected CompoundValidationStatus getActionStatus() {
		return CompoundValidationStatus.VALIDATED;
	}
}
