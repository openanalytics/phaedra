package eu.openanalytics.phaedra.ui.columbus.importwizard;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.MessageDialog;

import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.base.ui.util.wizard.BaseStatefulWizard;
import eu.openanalytics.phaedra.datacapture.columbus.ColumbusService;
import eu.openanalytics.phaedra.link.importer.ImportService;
import eu.openanalytics.phaedra.link.importer.ImportTask;
import eu.openanalytics.phaedra.link.importer.ImportUtils;
import eu.openanalytics.phaedra.model.protocol.vo.Protocol;
import eu.openanalytics.phaedra.ui.link.importer.wizard.GenericImportWizard.ImportWizardState;

public class ColumbusImportWizard extends BaseStatefulWizard {
	
	private SelectProtocol page1;
	private SelectSourceColumbus page2;
	private SelectExperiment page3;
	
	public ColumbusImportWizard() {
		setWindowTitle("Columbus Importer");
		setNeedsProgressMonitor(true);
	}

	@Override
	public void addPages() {
		page1 = new SelectProtocol();
		page2 = new SelectSourceColumbus();
		page3 = new SelectExperiment();

		addPage(page1);
		addPage(page2);
		addPage(page3);
	}

	@SuppressWarnings("unchecked")
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
		
		if (!checkFinish(ImportUtils.precheckTask(task))) {
			return false;
		}

		// The results to download, see ColumbusDownloadModule.
		Map<Long, Long> resultIds = new HashMap<>();
		List<Meas> measurements = (List<Meas>) task.getParameters().get(OperaImportHelper.PARAM_MEAS_SOURCES);
		for (Meas meas: measurements) {
			if (!meas.isIncluded || meas.selectedAnalysis == null) continue;
			resultIds.put(Long.parseLong(meas.source), Long.parseLong(meas.selectedAnalysis.source));
		}
		ColumbusService.getInstance().setResultIds(task.getParameters(), resultIds);

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
