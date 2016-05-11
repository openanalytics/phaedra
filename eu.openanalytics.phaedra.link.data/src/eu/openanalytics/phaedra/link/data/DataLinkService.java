package eu.openanalytics.phaedra.link.data;

import java.util.ArrayList;
import java.util.HashMap;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;

import eu.openanalytics.phaedra.datacapture.DataCaptureService;

/**
 * Entry point for linking readings to plates.
 * A reading can be linked into a new or an existing plate.
 * The target experiment must exist before the task can be executed.
 */
public class DataLinkService {

	private static DataLinkService instance = new DataLinkService();
	
	public DataLinkService() {
		// Hidden constructor
		DataCaptureService.getInstance().addLogListener(new AutoDataLinker());
	}
	
	public static DataLinkService getInstance() {
		return instance;
	}
	
	public DataLinkTask createTask() {
		DataLinkTask task = new DataLinkTask();
		task.createNewPlates = true;
		task.linkPlateData = true;
		task.linkWellData = true;
		task.linkImageData = true;
		task.linkSubWellData = true;
		task.mappedReadings = new HashMap<>();
		task.selectedReadings = new ArrayList<>();
		return task;
	}

	public void executeTaskInJob(final DataLinkTask task) {
		Job job = new Job("Data Link") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				return executeTask(task, monitor);
			}
		};
		job.schedule();	
	}
	
	public IStatus executeTask(DataLinkTask task, IProgressMonitor monitor) {
		DataLinker linker = new DataLinker();
		linker.setTask(task);
		if (monitor == null) monitor = new NullProgressMonitor();
		return linker.execute(monitor);
	}
	
	public boolean queueTask(DataLinkTask task) {
		return DataLinkTaskQueue.submit(task);
	}
}
