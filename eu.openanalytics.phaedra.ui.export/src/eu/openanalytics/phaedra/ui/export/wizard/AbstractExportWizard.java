package eu.openanalytics.phaedra.ui.export.wizard;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import eu.openanalytics.phaedra.export.core.IExportExperimentsSettings;
import eu.openanalytics.phaedra.ui.export.Activator;


public abstract class AbstractExportWizard extends Wizard {

	public AbstractExportWizard(String title) {
		setWindowTitle(title);
	}

	protected abstract IExportExperimentsSettings getSettings();

	protected abstract Job createExportJob();

	protected void setDialogSettings(String sectionKey) {
		IDialogSettings dialogSettings = Activator.getDefault().getDialogSettings();
		IDialogSettings section = dialogSettings.getSection(sectionKey);
		if (section == null) {
			section = dialogSettings.addNewSection(sectionKey);
		}
		super.setDialogSettings(section);
	}

	@Override
	public boolean performFinish() {
		for (IWizardPage page : getPages()) {
			if (page instanceof IExportWizardPage) {
				IExportWizardPage exportPage= (IExportWizardPage)page;
				exportPage.collectSettings();
				exportPage.saveDialogSettings();
			}
		}
		
		// Trigger a new Export Job
		Job exportJob = createExportJob();
		exportJob.setUser(true);
		exportJob.schedule();
		exportJob.addJobChangeListener(new JobChangeAdapter() {
			@Override
			public void done(IJobChangeEvent event) {
				if (event.getResult().getSeverity() == IStatus.OK) {
					showOutcomeDialog();
				}
			}
		});
		return true;
	}

	private void showOutcomeDialog() {
		Display.getDefault().asyncExec(() -> {
			Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
			ExportOutcomeDialog dialog = new ExportOutcomeDialog(shell, getSettings());
			dialog.open();
		});
	}
	
}
