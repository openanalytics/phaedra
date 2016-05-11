package eu.openanalytics.phaedra.ui.protocol.cmd;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;

import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.base.security.model.Permissions;
import eu.openanalytics.phaedra.ui.protocol.wizard.ProtocolWizard;

public class StartProtocolWizard extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		if (SecurityService.getInstance().checkWithDialog(Permissions.PROTOCOLCLASS_CREATE, null)) {
			ProtocolWizard wizard = new ProtocolWizard(null);
			WizardDialog dialog = new WizardDialog(Display.getDefault().getActiveShell(), wizard);
			dialog.open();
		}
		return null;
	}

}
