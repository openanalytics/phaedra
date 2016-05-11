package eu.openanalytics.phaedra.ui.curve.cmd;

import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;

import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.base.security.model.Permissions;
import eu.openanalytics.phaedra.model.plate.vo.Compound;

public abstract class AbstractCompoundCmd extends AbstractHandler implements IHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		List<Compound> compounds = CmdUtil.getCompounds(event);
		if (compounds.isEmpty()) return null;
		
		// Security check.
		for (Compound c: compounds) {
			boolean permission = SecurityService.getInstance().checkWithDialog(Permissions.COMPOUND_CHANGE_VALIDATION, c);
			if (!permission) return null;
		}

		execInternal(compounds);
		return null;
	}
	
	protected abstract void execInternal(List<Compound> compounds);
}
