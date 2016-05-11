package eu.openanalytics.phaedra.ui.curve.cmd;

import java.util.List;

import eu.openanalytics.phaedra.model.plate.vo.Compound;
import eu.openanalytics.phaedra.validation.ValidationJobHelper;
import eu.openanalytics.phaedra.validation.ValidationService.Action;


public class ResetCompoundValidation extends AbstractCompoundCmd {

	@Override
	protected void execInternal(List<Compound> compounds) {
		ValidationJobHelper.doInJob(Action.RESET_COMPOUND_VALIDATION, null, compounds);
	}
}
