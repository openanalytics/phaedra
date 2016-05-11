package eu.openanalytics.phaedra.ui.curve.cmd;

import java.util.List;

import eu.openanalytics.phaedra.model.plate.vo.Compound;
import eu.openanalytics.phaedra.validation.ValidationJobHelper;
import eu.openanalytics.phaedra.validation.ValidationService.Action;

public class ValidateCompound extends AbstractCompoundCmd {

	@Override
	protected void execInternal(List<Compound> compounds) {
		ValidationJobHelper.doInJob(Action.VALIDATE_COMPOUND, null, compounds);
	}
}
