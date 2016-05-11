package eu.openanalytics.phaedra.ui.protocol.template;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

import eu.openanalytics.phaedra.base.ui.editor.EditorFactory;
import eu.openanalytics.phaedra.base.ui.util.wizard.BaseStatefulWizard;
import eu.openanalytics.phaedra.base.ui.util.wizard.IWizardState;
import eu.openanalytics.phaedra.model.protocol.vo.Protocol;
import eu.openanalytics.phaedra.protocol.template.ProtocolTemplateException;
import eu.openanalytics.phaedra.protocol.template.ProtocolTemplateService;

public class TemplateWizard extends BaseStatefulWizard {

	public TemplateWizard() {
		setPreconfiguredState(new WizardState());
	}
	
	@Override
	public void addPages() {
		addPage(new TemplateWizardPage());
	}
	
	@Override
	public boolean performFinish() {
		IStatus status = runTemplate(((WizardState) state).settings, new NullProgressMonitor());
		if (status.getSeverity() == IStatus.ERROR) {
			MessageDialog.openError(getShell(), "Template Error",
					"An error occurred while executing the template:\n" + status.getMessage());
		}
		return status.isOK();
	}

	public static class WizardState implements IWizardState {
		public String settings;
	}
	
	private IStatus runTemplate(String settings, IProgressMonitor monitor) {
		try {
			Protocol newProtocol = ProtocolTemplateService.getInstance().executeTemplate(settings, monitor);
			if (monitor.isCanceled()) return Status.CANCEL_STATUS;
			String msg = "The Protocol has been created successfully:\n"
					+ "Name: " + newProtocol.getName() + "\n"
					+ "ID: " + newProtocol.getId();
			Display.getDefault().syncExec(() -> {
				MessageDialog.openInformation(getShell(), "Protocol Created", msg);
				EditorFactory.getInstance().openEditor(newProtocol);
			});
			return Status.OK_STATUS;
		} catch (ProtocolTemplateException e) {
			return new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage(), e);
		}
	}
}
