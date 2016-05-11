package eu.openanalytics.phaedra.datacapture;

import org.eclipse.core.runtime.NullProgressMonitor;

public class DataCaptureProgressMonitor extends NullProgressMonitor {

	private double progress = 0;
	private double totalWork = 100;
	private String activeSubTask = "";
	
	public double getProgress() {
		// Gives the client the completion percentage
		return progress / totalWork * 100;
	}
	
	public String getSubTask() {
		return activeSubTask;
	}
	
	@Override
	public void beginTask(String name, int totalWork) {
		this.totalWork = totalWork <= 0? 1 : totalWork;
	}
	
	@Override
	public void internalWorked(double work) {
		if (work <= 0) return;
		if (work + progress > totalWork) work = totalWork - progress;
		progress = progress + work;
	}		
	
	@Override
	public void worked(int work) {
		internalWorked(work);
	}

	@Override
	public void subTask(String name) {
		activeSubTask = name;
	}
}
