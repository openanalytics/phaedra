package eu.openanalytics.phaedra.ui.columbus.importwizard;

import org.eclipse.jface.dialogs.MessageDialog;

import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.base.ui.util.wizard.BaseStatefulWizard;
import eu.openanalytics.phaedra.link.importer.ImportService;
import eu.openanalytics.phaedra.link.importer.ImportTask;
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;
import eu.openanalytics.phaedra.model.protocol.vo.Protocol;
import eu.openanalytics.phaedra.ui.link.importer.wizard.GenericImportWizard.ImportWizardState;

public class OperaImportWizard extends BaseStatefulWizard {

	private SelectProtocol page1;
	private SelectSourceOpera page2;
	private SelectExperiment page3;

	public OperaImportWizard() {
		setWindowTitle("Opera/Acapella Importer");
		setNeedsProgressMonitor(true);
	}

	@Override
	public void addPages() {
		page1 = new SelectProtocol();
		page2 = new SelectSourceOpera();
		page3 = new SelectExperiment();

		addPage(page1);
		addPage(page2);
		addPage(page3);
	}

	@Override
	public boolean performFinish() {
		super.performFinish();
		
		ImportWizardState state = (ImportWizardState)getState();
		ImportTask task = state.task;
		task.userName = SecurityService.getInstance().getCurrentUserName();

		// If user requested a new experiment, create it now.
		if (task.targetExperiment == null) {
			Protocol protocol = (Protocol)task.getParameters().get(OperaImportHelper.PARAM_PROTOCOL);
			Experiment experiment = PlateService.getInstance().createExperiment(protocol);
			String newExperimentName = (String)task.getParameters().get(OperaImportHelper.PARAM_EXPERIMENT_NAME);
			experiment.setName(newExperimentName);
			try {
				PlateService.getInstance().updateExperiment(experiment);
			} catch (Throwable t) {
				MessageDialog.openError(getShell(), "Error!", "Failed to create Experiment.\n\n" + t.getMessage());
				return false;
			}
			task.targetExperiment = experiment;
		}

		// Add again as parameters, because some modules (that can't see the ImportTask) need access to it.
		task.getParameters().put(OperaImportHelper.PARAM_IMPORT_IMG_DATA, task.importImageData);
		task.getParameters().put(OperaImportHelper.PARAM_IMPORT_SW_DATA, task.importSubWellData);

		ImportService.getInstance().startJob(task);
		return true;
	}

}
