package eu.openanalytics.phaedra.ui.export.wizard;

import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import eu.openanalytics.phaedra.export.core.ExportJob;
import eu.openanalytics.phaedra.export.core.ExportSettings;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;
import eu.openanalytics.phaedra.ui.export.wizard.pages.FilterPlatesPage;
import eu.openanalytics.phaedra.ui.export.wizard.pages.FilterWellsPage;
import eu.openanalytics.phaedra.ui.export.wizard.pages.IncludeDataPage;
import eu.openanalytics.phaedra.ui.export.wizard.pages.SelectFeaturePage;

public class ExportWizard extends Wizard {

	private ExportSettings settings;

	public ExportWizard(List<Experiment> experiments) {
		setWindowTitle("Export Wizard");

		settings = new ExportSettings();
		settings.experiments = experiments;
	}

	@Override
	public void addPages() {
		addPage(new SelectFeaturePage());
		addPage(new FilterPlatesPage());
		addPage(new FilterWellsPage());
		addPage(new IncludeDataPage());
	}

	@Override
	public IWizardPage getNextPage(IWizardPage page) {
		// Collect the settings from the previous page
		if (page != null && page instanceof IExportWizardPage) {
			((IExportWizardPage)page).collectSettings(settings);
		}

		return super.getNextPage(page);
	}

	@Override
	public boolean performFinish() {

		// Make sure to collect settings from the last page
		IWizardPage page = getPages()[getPageCount()-1];
		if (page != null && page instanceof IExportWizardPage) {
			((IExportWizardPage)page).collectSettings(settings);
		}

		// Trigger a new Export Job
		Job exportJob = new ExportJob(settings);
		exportJob.setUser(true);
		exportJob.schedule();
		exportJob.addJobChangeListener(new JobChangeAdapter() {
			@Override
			public void done(IJobChangeEvent event) {
				if (event.getResult().getSeverity() == IStatus.OK) {
					showOutcomeDialog(settings);
				}
			}
		});
		return true;
	}

	protected ExportSettings getSettings() {
		return settings;
	}

	private void showOutcomeDialog(final ExportSettings settings) {
		Display.getDefault().asyncExec(() -> {
			Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
			ExportOutcomeDialog dialog = new ExportOutcomeDialog(shell, settings);
			dialog.open();
		});
	}
}
