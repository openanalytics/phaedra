package eu.openanalytics.phaedra.link.importer;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.base.security.model.Permissions;
import eu.openanalytics.phaedra.model.plate.vo.Plate;

/**
 * This service enables the submission of import jobs.
 * An import job is a combination of two jobs:
 * <ul>
 * <li>A Data Capture job, which parses the input data and creates reading objects</li>
 * <li>A Data Link job, which places the data from the reading into a plate object (a new or existing plate)</li>
 * </ul>
 * If multiple import jobs are submitted, they are placed in a queue and processed one by one.
 */
public class ImportService {

	private static ImportService instance = new ImportService();
	
	private ImportService() {
		// Hidden constructor
	}
	
	public static ImportService getInstance() {
		return instance;
	}
	
	/*
	 * **********
	 * Public API
	 * **********
	 */
	
	public IStatus startJob(ImportTask task) {
		IStatus status;
		
		// Security check first.
		boolean access = false;
		if (task.createNewPlates) {
			access = SecurityService.getInstance().checkWithDialog(Permissions.PLATE_EDIT, task.targetExperiment);
		} else {
			for (Plate p: task.plateMapping.values()) {
				access = SecurityService.getInstance().checkWithDialog(Permissions.PLATE_EDIT, p);
				if (!access) break;
			}
		}
		if (!access) return Status.CANCEL_STATUS;
		
		status = ImportUtils.precheckTask(task);
		if (status.getSeverity() >= IStatus.ERROR) {
			return status;
		}
		
		// Open the log view, if present.
		try {
			String viewId = "eu.openanalytics.phaedra.ui.link.importer.view.DataCaptureLogView";
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(viewId);
		} catch (PartInitException e) {}
		
		Job importJob = new ImportJob(task);
		importJob.setRule(new ImportJobRule());
		importJob.setUser(true);
		importJob.schedule();
		return Status.OK_STATUS;
	}
	
	private static class ImportJobRule implements ISchedulingRule {
		@Override
		public boolean contains(ISchedulingRule rule) {
			return this == rule;
		}
		
		@Override
		public boolean isConflicting(ISchedulingRule rule) {
			return (rule instanceof ImportJobRule);
		}
	}
}
