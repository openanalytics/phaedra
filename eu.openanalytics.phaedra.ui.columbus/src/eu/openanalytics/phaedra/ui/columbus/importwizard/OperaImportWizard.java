package eu.openanalytics.phaedra.ui.columbus.importwizard;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.MessageDialog;

import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.base.ui.util.wizard.BaseStatefulWizard;
import eu.openanalytics.phaedra.link.importer.ImportService;
import eu.openanalytics.phaedra.link.importer.ImportTask;
import eu.openanalytics.phaedra.link.importer.ImportUtils;
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
			if (!checkFinish(ImportUtils.createExperiment(task,
					(Protocol)task.getParameters().get(OperaImportHelper.PARAM_PROTOCOL),
					(String)task.getParameters().get(OperaImportHelper.PARAM_EXPERIMENT_NAME) ))) {
				return false;
			}
		}
		
		// Add again as parameters, because some modules (that can't see the ImportTask) need access to it.
		task.getParameters().put(OperaImportHelper.PARAM_IMPORT_IMG_DATA, task.importImageData);
		task.getParameters().put(OperaImportHelper.PARAM_IMPORT_SW_DATA, task.importSubWellData);

		return checkFinish(
				ImportService.getInstance().startJob(task) );
	}
	
	private boolean checkFinish(final IStatus status) {
		if (status.getSeverity() == IStatus.ERROR) {
			MessageDialog.openError(getShell(), "Import", status.getMessage());
		}
		return (status.getSeverity() < IStatus.ERROR);
	}

}
