package eu.openanalytics.phaedra.validation.action.compound;

import eu.openanalytics.phaedra.validation.ValidationService.CompoundValidationStatus;


public class InvalidateCompoundAction extends AbstractCompoundAction {

	@Override
	protected CompoundValidationStatus getActionStatus() {
		return CompoundValidationStatus.INVALIDATED;
	}
}
