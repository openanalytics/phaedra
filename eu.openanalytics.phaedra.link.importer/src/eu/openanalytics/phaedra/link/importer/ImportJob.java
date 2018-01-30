package eu.openanalytics.phaedra.link.importer;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;

import eu.openanalytics.phaedra.datacapture.DataCaptureService;
import eu.openanalytics.phaedra.datacapture.DataCaptureTask;
import eu.openanalytics.phaedra.datacapture.DataCaptureTask.DataCaptureParameter;
import eu.openanalytics.phaedra.link.importer.preferences.Prefs;

public class ImportJob extends Job {

	private ImportTask task;

	public ImportJob(ImportTask task) {
		super("Import" + (task.sourcePath == null ? "" : ": " + task.sourcePath));
		this.task = task;
	}

	@Override
	public boolean belongsTo(Object family) {
		if (family != null && family.equals("ImportJobFamily"))
			return true;
		return false;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		SubMonitor mon = SubMonitor.convert(monitor);
		if (mon.isCanceled()) return doCancel();
		try {
			mon.beginTask("Importing", 100);
			DataCaptureTask captureTask = createCaptureTask();
			DataCaptureService.getInstance().executeTask(captureTask, mon.split(99));
			if (mon.isCanceled()) return doCancel();
		} catch (Throwable e) {
			return doError(e);
		}
		return Status.OK_STATUS;
	}

	private DataCaptureTask createCaptureTask() {

		DataCaptureTask captureTask = DataCaptureService.getInstance().createTask(task.sourcePath, task.getCaptureConfigId());
		if (captureTask.getConfigId() == null) {
			throw new RuntimeException("No capture configuration found for protocol " + task.targetExperiment.getProtocol().getName());
		}
		
		// Pass all import parameters as capture parameters, for the modules who might need it.
		captureTask.getParameters().putAll(task.getParameters());
		captureTask.getParameters().put(DataCaptureParameter.TargetExperiment.name(), task.targetExperiment);
		
		boolean detectWellFeatures = Activator.getDefault().getPreferenceStore().getBoolean(Prefs.DETECT_WELL_FEATURES);
		boolean detectSubWellFeatures = Activator.getDefault().getPreferenceStore().getBoolean(Prefs.DETECT_SUBWELL_FEATURES);
		captureTask.getParameters().put(DataCaptureParameter.CreateMissingWellFeatures.name(), detectWellFeatures);
		captureTask.getParameters().put(DataCaptureParameter.CreateMissingSubWellFeatures.name(), detectSubWellFeatures);

		if (!task.createNewPlates) {
			captureTask.getParameters().put(DataCaptureParameter.PlateMapping.name(), task.plateMapping);
		}
		
		String[] filter = ImportUtils.createFilter(
				task.getCaptureConfigId(),
				task.importWellData,
				task.importSubWellData,
				task.importImageData);
		captureTask.setModuleFilter(filter);

		return captureTask;
	}

	private IStatus doCancel() {
		return Status.CANCEL_STATUS;
	}

	private IStatus doError(Throwable e) {
		return new Status(IStatus.ERROR, Activator.getDefault().getBundle().getSymbolicName(), e.getMessage(),
				e.getCause());
	}
}
