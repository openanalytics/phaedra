package eu.openanalytics.phaedra.export.core;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import eu.openanalytics.phaedra.export.Activator;

public class ExportWellsJob extends Job {

	private ExportSettings exportSettings;
	
	public ExportWellsJob(ExportSettings exportSettings) {
		super("Export Well Data");
		this.exportSettings = exportSettings;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		Exporter exporter = new Exporter();
		try {
			exporter.export(exportSettings, monitor);
			
			if (monitor.isCanceled()) {
				return Status.CANCEL_STATUS;
			}
		} catch (ExportException e) {
			return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "The export failed due to an exception", e);
		}
		return Status.OK_STATUS;
	}

}
