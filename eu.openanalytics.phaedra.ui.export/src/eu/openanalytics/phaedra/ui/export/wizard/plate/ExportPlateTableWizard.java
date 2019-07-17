package eu.openanalytics.phaedra.ui.export.wizard.plate;

import java.util.List;

import org.eclipse.core.runtime.jobs.Job;

import eu.openanalytics.phaedra.export.core.ExportPlateTableJob;
import eu.openanalytics.phaedra.export.core.ExportPlateTableSettings;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;
import eu.openanalytics.phaedra.ui.export.wizard.AbstractExportWizard;
import eu.openanalytics.phaedra.ui.export.wizard.FilterPlatesPage;

public class ExportPlateTableWizard extends AbstractExportWizard {
	
	private ExportPlateTableSettings settings;
	
	
	public ExportPlateTableWizard(List<Experiment> experiments) {
		super("Export Plate List");
		
		settings = new ExportPlateTableSettings(experiments);
		setDialogSettings("ExportPlateTable");
	}
	
	
	@Override
	protected ExportPlateTableSettings getSettings() {
		return settings;
	}
	
	@Override
	protected Job createExportJob() {
		return new ExportPlateTableJob(getSettings());
	}
	
	@Override
	public void addPages() {
		int numStep = 1;
		int stepTotal = 3;
		addPage(new ExperimentsPage(settings, numStep++, stepTotal));
		addPage(new FilterPlatesPage(settings, numStep++, stepTotal));
		addPage(new IncludeDataPage(settings, numStep++, stepTotal));
	}
	
}
