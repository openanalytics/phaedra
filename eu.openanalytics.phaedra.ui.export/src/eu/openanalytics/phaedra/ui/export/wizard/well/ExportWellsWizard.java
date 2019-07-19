package eu.openanalytics.phaedra.ui.export.wizard.well;

import java.util.List;

import org.eclipse.core.runtime.jobs.Job;

import eu.openanalytics.phaedra.export.core.ExportSettings;
import eu.openanalytics.phaedra.export.core.ExportWellsJob;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;
import eu.openanalytics.phaedra.ui.export.Activator;
import eu.openanalytics.phaedra.ui.export.wizard.AbstractExportWizard;
import eu.openanalytics.phaedra.ui.export.wizard.FilterPlatesPage;

public class ExportWellsWizard extends AbstractExportWizard {

	private ExportSettings settings;

	public ExportWellsWizard(List<Experiment> experiments) {
		super("Export Well Data");
		
		settings = new ExportSettings(experiments);
		setDialogSettings(Activator.getDefault().getDialogSettings());
	}

	@Override
	protected ExportSettings getSettings() {
		return settings;
	}

	@Override
	protected Job createExportJob() {
		return new ExportWellsJob(getSettings());
	}

	@Override
	public void addPages() {
		addPage(new SelectFeaturePage(settings));
		addPage(new FilterPlatesPage(settings, 2, 4));
		addPage(new FilterWellsPage(settings));
		addPage(new IncludeDataPage(settings, 4, 4));
	}

}
