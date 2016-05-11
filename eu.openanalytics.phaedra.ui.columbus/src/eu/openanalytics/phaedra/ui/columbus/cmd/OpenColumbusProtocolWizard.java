package eu.openanalytics.phaedra.ui.columbus.cmd;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;

import eu.openanalytics.phaedra.ui.columbus.protocolwizard.ColumbusProtocolWizard;

public class OpenColumbusProtocolWizard extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ColumbusProtocolWizard wizard = new ColumbusProtocolWizard();
		WizardDialog dialog = new WizardDialog(Display.getDefault().getActiveShell(), wizard);
		dialog.open();
		return null;
	}

}
