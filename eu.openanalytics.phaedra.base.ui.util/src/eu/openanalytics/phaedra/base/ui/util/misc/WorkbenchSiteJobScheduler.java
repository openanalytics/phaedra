package eu.openanalytics.phaedra.base.ui.util.misc;

import java.util.function.Consumer;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.progress.IProgressService;
import org.eclipse.ui.progress.IWorkbenchSiteProgressService;


public class WorkbenchSiteJobScheduler implements Consumer<Job> {
	
	
	private final IWorkbenchPart part;
	
	
	public WorkbenchSiteJobScheduler(final IWorkbenchPart part) {
		this.part = part;
	}
	
	
	@Override
	public void accept(final Job job) {
		final IWorkbenchPartSite site = this.part.getSite();
		if (site != null) {
			final IProgressService service = site.getService(IProgressService.class);
			if (service instanceof IWorkbenchSiteProgressService) {
				((IWorkbenchSiteProgressService) service).schedule(job);
				return;
			}
		}
		job.schedule();
	}
	
}
